package com.nexus.app.providers.compat

import com.nexus.app.providers.Capabilities
import com.nexus.app.providers.ChatEvent
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

open class OpenAiCompatibleAdapter(
    private val client: OkHttpClient,
    private val config: ProviderConfig
) : ProviderAdapter {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val baseUrl: String = config.baseUrl.trimEnd('/')

    override val capabilities: Capabilities = Capabilities(
        streaming = true, vision = false, tools = true, reasoning = true, citations = false, files = false
    )

    open val fallbackModels: List<ModelInfo> = emptyList()

    override suspend fun listModels(): List<ModelInfo> {
        val req = Request.Builder()
            .url("$baseUrl/models")
            .apply {
                if (config.apiKey.isNotBlank()) header("Authorization", "Bearer ${config.apiKey}")
            }
            .get()
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw classifyHttp(resp.code, resp.message)
                val body = resp.body?.string().orEmpty()
                val parsed = json.decodeFromString<ModelList>(body)
                val fromApi = parsed.data.map { ModelInfo(it.id, it.id) }
                if (fromApi.isNotEmpty()) fromApi else fallbackModels
            }
        } catch (e: ProviderError) {
            fallbackModels
        } catch (e: IOException) {
            fallbackModels
        } catch (e: Exception) {
            fallbackModels
        }
    }

    override suspend fun testConnection(): Result<Unit> = try {
        val req = Request.Builder()
            .url("$baseUrl/models")
            .apply {
                if (config.apiKey.isNotBlank()) header("Authorization", "Bearer ${config.apiKey}")
            }
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (resp.isSuccessful) Result.success(Unit)
            else Result.failure(classifyHttp(resp.code, resp.message))
        }
    } catch (e: ProviderError) {
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(ProviderError.Network(e.message ?: "Network error", e))
    } catch (e: Throwable) {
        Result.failure(ProviderError.Unknown(e.message ?: "Unknown error", e))
    }

    override fun streamChat(request: ChatRequest): Flow<ChatEvent> = callbackFlow {
        val payload = buildPayload(request)
        val req = Request.Builder()
            .url("$baseUrl/chat/completions")
            .apply {
                if (config.apiKey.isNotBlank()) header("Authorization", "Bearer ${config.apiKey}")
                header("Content-Type", "application/json")
                header("Accept", "text/event-stream")
            }
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val source = EventSources.createFactory(client).newEventSource(req, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (data == "[DONE]") {
                    trySend(ChatEvent.Done)
                    close()
                    return
                }
                try {
                    val chunk = json.decodeFromString<StreamChunk>(data)
                    val delta = chunk.choices.firstOrNull()?.delta
                    val reasoning = delta?.reasoning ?: delta?.reasoningContent
                    if (!reasoning.isNullOrEmpty()) {
                        trySend(ChatEvent.Reasoning(reasoning))
                    }
                    if (!delta?.content.isNullOrEmpty()) {
                        trySend(ChatEvent.Token(delta.content))
                    }
                    if (chunk.choices.firstOrNull()?.finishReason != null) {
                        trySend(ChatEvent.Done)
                        close()
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
        val messages = buildList {
            request.systemPrompt?.takeIf { it.isNotBlank() }?.let {
                add(ApiMessage("system", it))
            }
            request.messages.forEach { m ->
                add(ApiMessage(m.role.name.lowercase(), m.content))
            }
        }
        val body = ApiRequest(
            model = request.model,
            messages = messages,
            stream = true,
            temperature = request.temperature ?: config.temperature,
            maxTokens = request.maxTokens ?: config.maxTokens,
            reasoningEffort = request.reasoningEffort ?: config.reasoningEffort
        )
        return json.encodeToString(ApiRequest.serializer(), body)
    }

    /**
     * Maps an HTTP status to a typed `ProviderError`. When this adapter is
     * running under the NVIDIA NIM preset, 401/429/404 use the preset's exact
     * copy ("Invalid NVIDIA key", etc.). Everything else falls back to the
     * generic text used by every other OpenAI-compatible provider.
     */
    private fun classifyHttp(code: Int, message: String): ProviderError {
        val presetMessage = if (config.presetId == NimPreset.ID) {
            NimPreset.errorMessageForStatus(code)
        } else {
            null
        }
        return when (code) {
            401, 403 -> ProviderError.Unauthorized(presetMessage ?: "Unauthorized: $message")
            429 -> ProviderError.RateLimited(presetMessage ?: "Rate limited: $message")
            404 -> ProviderError.Unknown(presetMessage ?: "Not found: $message")
            in 500..599 -> ProviderError.Server("Server error $code: $message")
            else -> ProviderError.Unknown(presetMessage ?: "HTTP $code: $message")
        }
    }

    @Serializable
    private data class ApiRequest(
        val model: String,
        val messages: List<ApiMessage>,
        val stream: Boolean = true,
        val temperature: Float? = null,
        @SerialName("max_tokens") val maxTokens: Int? = null,
        @SerialName("reasoning_effort") val reasoningEffort: String? = null
    )

    @Serializable
    private data class ApiMessage(val role: String, val content: String)

    @Serializable
    private data class StreamChunk(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(
        val delta: Delta? = null,
        @SerialName("finish_reason") val finishReason: String? = null
    )

    @Serializable
    private data class Delta(
        val content: String? = null,
        val reasoning: String? = null,
        @SerialName("reasoning_content") val reasoningContent: String? = null
    )

    @Serializable
    private data class ModelList(val data: List<ModelDto> = emptyList())

    @Serializable
    private data class ModelDto(val id: String)
}