package com.nexus.app.providers.anthropic

import com.nexus.app.providers.Capabilities
import com.nexus.app.providers.ChatEvent
import com.nexus.app.providers.ChatMessage
import com.nexus.app.providers.ChatRequest
import com.nexus.app.providers.ModelInfo
import com.nexus.app.providers.ProviderAdapter
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.ProviderError
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException

class AnthropicAdapter(
    private val client: OkHttpClient,
    private val config: ProviderConfig
) : ProviderAdapter {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val baseUrl: String = config.baseUrl.trimEnd('/')

    override val capabilities: Capabilities = Capabilities(
        streaming = true, vision = true, tools = true, reasoning = true, citations = true, files = true
    )

    override suspend fun listModels(): List<ModelInfo> {
        val req = Request.Builder()
            .url("$baseUrl/v1/models")
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .get()
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw classifyHttp(resp.code, resp.message)
                val body = resp.body?.string().orEmpty()
                val parsed = json.decodeFromString<ModelList>(body)
                parsed.data.map { ModelInfo(it.id, it.displayName ?: it.id) }.ifEmpty { fallbackModels() }
            }
        } catch (e: ProviderError) {
            fallbackModels()
        } catch (e: IOException) {
            fallbackModels()
        }
    }

    override suspend fun testConnection(): Result<Unit> = try {
        listModels()
        Result.success(Unit)
    } catch (e: ProviderError) {
        Result.failure(e)
    } catch (e: Throwable) {
        Result.failure(ProviderError.Unknown(e.message ?: "Unknown error", e))
    }

    override fun streamChat(request: ChatRequest): Flow<ChatEvent> = callbackFlow {
        val payload = buildPayload(request)
        val req = Request.Builder()
            .url("$baseUrl/v1/messages")
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .header("accept", "text/event-stream")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val source = EventSources.createFactory(client).newEventSource(req, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val event = json.decodeFromString<StreamEvent>(data)
                    when (event.type) {
                        "content_block_delta" -> {
                            val delta = event.delta
                            if (delta?.type == "text_delta" && !delta.text.isNullOrEmpty()) {
                                trySend(ChatEvent.Token(delta.text))
                            } else if (delta?.type == "thinking_delta" && !delta.thinking.isNullOrEmpty()) {
                                trySend(ChatEvent.Reasoning(delta.thinking))
                            }
                        }
                        "message_stop" -> {
                            trySend(ChatEvent.Done)
                            close()
                        }
                        "error" -> {
                            trySend(ChatEvent.Error(event.error?.message ?: "Stream error"))
                            close()
                        }
                    }
                } catch (e: Exception) {
                    trySend(ChatEvent.Error("Parse error: ${e.message}", e))
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val err = if (response != null) classifyHttp(response.code, response.message)
                else ProviderError.Network(t?.message ?: "Network error", t)
                trySend(ChatEvent.Error(err.message ?: "Error", err))
                close()
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        })

        awaitClose { source.cancel() }
    }

    private fun buildPayload(request: ChatRequest): String {
        val system = request.systemPrompt?.takeIf { it.isNotBlank() }
        val messages = request.messages
            .filter { it.role != ChatMessage.Role.SYSTEM }
            .map { ApiMessage(it.role.name.lowercase(), it.content) }
        val body = ApiRequest(
            model = request.model,
            maxTokens = request.maxTokens ?: config.maxTokens ?: 1024,
            system = system,
            messages = messages,
            stream = true,
            temperature = request.temperature ?: config.temperature
        )
        return json.encodeToString(ApiRequest.serializer(), body)
    }

    private fun classifyHttp(code: Int, message: String): ProviderError = when (code) {
        401, 403 -> ProviderError.Unauthorized("Unauthorized: $message")
        429 -> ProviderError.RateLimited("Rate limited: $message")
        in 500..599 -> ProviderError.Server("Server error $code: $message")
        else -> ProviderError.Unknown("HTTP $code: $message")
    }

    private fun fallbackModels(): List<ModelInfo> = listOf(
        ModelInfo("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", supportsVision = true, supportsTools = true),
        ModelInfo("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", supportsVision = true, supportsTools = true),
        ModelInfo("claude-3-opus-20240229", "Claude 3 Opus", supportsVision = true, supportsTools = true)
    )

    @Serializable
    private data class ApiRequest(
        val model: String,
        @SerialName("max_tokens") val maxTokens: Int,
        val system: String? = null,
        val messages: List<ApiMessage>,
        val stream: Boolean = true,
        val temperature: Float? = null
    )

    @Serializable
    private data class ApiMessage(val role: String, val content: String)

    @Serializable
    private data class StreamEvent(
        val type: String,
        val delta: Delta? = null,
        val error: ErrorPayload? = null
    )

    @Serializable
    private data class Delta(
        val type: String? = null,
        val text: String? = null,
        val thinking: String? = null
    )

    @Serializable
    private data class ErrorPayload(val type: String? = null, val message: String? = null)

    @Serializable
    private data class ModelList(val data: List<ModelDto> = emptyList())

    @Serializable
    private data class ModelDto(
        val id: String,
        @SerialName("display_name") val displayName: String? = null
    )
}