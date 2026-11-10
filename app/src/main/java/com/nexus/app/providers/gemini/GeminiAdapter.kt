package com.nexus.app.providers.gemini

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

class GeminiAdapter(
    private val client: OkHttpClient,
    private val config: ProviderConfig
) : ProviderAdapter {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }
    private val baseUrl: String = config.baseUrl.trimEnd('/')

    override val capabilities: Capabilities = Capabilities(
        streaming = true, vision = true, tools = true, reasoning = false, citations = false, files = true
    )

    override suspend fun listModels(): List<ModelInfo> {
        val req = Request.Builder()
            .url("$baseUrl/v1beta/models?key=${config.apiKey}")
            .get()
            .build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw classifyHttp(resp.code, resp.message)
                val body = resp.body?.string().orEmpty()
                val parsed = json.decodeFromString<ModelList>(body)
                parsed.models
                    .filter { it.supportedGenerationMethods?.contains("generateContent") == true }
                    .map {
                        val shortId = it.name.removePrefix("models/")
                        ModelInfo(shortId, it.displayName ?: shortId)
                    }
                    .ifEmpty { fallbackModels() }
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
        val url = "$baseUrl/v1beta/models/${request.model}:streamGenerateContent?alt=sse&key=${config.apiKey}"
        val req = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()

        val source = EventSources.createFactory(client).newEventSource(req, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val chunk = json.decodeFromString<GeminiResponse>(data)
                    val candidate = chunk.candidates?.firstOrNull()
                    candidate?.content?.parts?.forEach { part ->
                        if (!part.text.isNullOrEmpty()) {
                            trySend(ChatEvent.Token(part.text))
                        }
                    }
                    if (candidate?.finishReason != null) {
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
        val contents = request.messages
            .filter { it.role != ChatMessage.Role.SYSTEM }
            .map {
                val role = if (it.role == ChatMessage.Role.ASSISTANT) "model" else "user"
                ReqContent(role = role, parts = listOf(ReqPart(text = it.content)))
            }
        val systemInstruction = request.systemPrompt?.takeIf { it.isNotBlank() }?.let {
            SystemInstruction(parts = listOf(ReqPart(text = it)))
        }
        val generationConfig = GenerationConfig(
            temperature = request.temperature ?: config.temperature,
            maxOutputTokens = request.maxTokens ?: config.maxTokens
        )
        val body = GeminiRequest(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = generationConfig
        )
        return json.encodeToString(GeminiRequest.serializer(), body)
    }

    private fun classifyHttp(code: Int, message: String): ProviderError = when (code) {
        401, 403 -> ProviderError.Unauthorized("Unauthorized: $message")
        429 -> ProviderError.RateLimited("Rate limited: $message")
        in 500..599 -> ProviderError.Server("Server error $code: $message")
        else -> ProviderError.Unknown("HTTP $code: $message")
    }

    private fun fallbackModels(): List<ModelInfo> = listOf(
        ModelInfo("gemini-1.5-flash", "Gemini 1.5 Flash", supportsVision = true, supportsTools = true),
        ModelInfo("gemini-1.5-pro", "Gemini 1.5 Pro", supportsVision = true, supportsTools = true),
        ModelInfo("gemini-2.0-flash-exp", "Gemini 2.0 Flash", supportsVision = true, supportsTools = true)
    )

    @Serializable
    private data class GeminiRequest(
        val contents: List<ReqContent>,
        @SerialName("systemInstruction") val systemInstruction: SystemInstruction? = null,
        @SerialName("generationConfig") val generationConfig: GenerationConfig? = null
    )

    @Serializable
    private data class ReqContent(val role: String, val parts: List<ReqPart>)

    @Serializable
    private data class ReqPart(val text: String)

    @Serializable
    private data class SystemInstruction(val parts: List<ReqPart>)

    @Serializable
    private data class GenerationConfig(
        val temperature: Float? = null,
        @SerialName("maxOutputTokens") val maxOutputTokens: Int? = null
    )

    @Serializable
    private data class GeminiResponse(val candidates: List<Candidate>? = null)

    @Serializable
    private data class Candidate(
        val content: RespContent? = null,
        @SerialName("finishReason") val finishReason: String? = null
    )

    @Serializable
    private data class RespContent(val parts: List<RespPart>? = null)

    @Serializable
    private data class RespPart(val text: String? = null)

    @Serializable
    private data class ModelList(val models: List<ModelDto> = emptyList())

    @Serializable
    private data class ModelDto(
        val name: String,
        @SerialName("displayName") val displayName: String? = null,
        @SerialName("supportedGenerationMethods") val supportedGenerationMethods: List<String>? = null
    )
}