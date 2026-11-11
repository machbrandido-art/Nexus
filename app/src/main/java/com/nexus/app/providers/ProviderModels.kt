package com.nexus.app.providers

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderType { ANTHROPIC, OPENAI, GEMINI, COMPAT }

@Serializable
data class ModelInfo(
    val id: String,
    val displayName: String = id,
    val supportsVision: Boolean = false,
    val supportsTools: Boolean = false,
    val contextWindow: Int? = null
)

data class Capabilities(
    val streaming: Boolean = true,
    val vision: Boolean = false,
    val tools: Boolean = false,
    val reasoning: Boolean = false,
    val citations: Boolean = false,
    val files: Boolean = false
)

data class ProviderConfig(
    val id: String,
    val name: String,
    val type: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val models: List<String> = emptyList(),
    val enabled: Boolean = true,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val reasoningEffort: String? = null,
    /**
     * Optional preset marker. `null` for hand-created providers. Presets that want
     * provider-specific behaviour (NVIDIA NIM custom error copy, etc.) set this,
     * and the adapter inspects it via the config.
     */
    val presetId: String? = null
)

data class ChatMessage(val role: Role, val content: String) {
    enum class Role { SYSTEM, USER, ASSISTANT }
}

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val systemPrompt: String? = null,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val reasoningEffort: String? = null
)

sealed class ChatEvent {
    data class Token(val text: String) : ChatEvent()
    data class Reasoning(val text: String) : ChatEvent()
    data class Citation(val index: Int, val title: String, val url: String?) : ChatEvent()
    data object Done : ChatEvent()
    data class Error(val message: String, val cause: Throwable? = null) : ChatEvent()
}

sealed class ProviderError(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    class Unauthorized(message: String = "Unauthorized") : ProviderError(message)
    class RateLimited(message: String = "Rate limited", val retryAfterSeconds: Long? = null) : ProviderError(message)
    class Network(message: String, cause: Throwable? = null) : ProviderError(message, cause)
    class Timeout(message: String = "Timeout") : ProviderError(message)
    class Server(message: String) : ProviderError(message)
    class Unknown(message: String, cause: Throwable? = null) : ProviderError(message, cause)
}