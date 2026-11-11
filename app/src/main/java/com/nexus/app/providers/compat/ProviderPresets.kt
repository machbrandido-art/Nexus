package com.nexus.app.providers.compat

import com.nexus.app.providers.ProviderType

data class ProviderPreset(
    val id: String,
    val name: String,
    val type: ProviderType,
    val defaultBaseUrl: String,
    val apiKeyHint: String? = null,
    val apiKeyRequired: Boolean = true,
    val defaultModels: List<String> = emptyList()
)

val AllProviderPresets: List<ProviderPreset> = listOf(
    ProviderPreset(
        id = NimPreset.ID,
        name = NimPreset.NAME,
        type = ProviderType.COMPAT,
        defaultBaseUrl = NimPreset.CLOUD_BASE_URL,
        apiKeyHint = NimPreset.API_KEY_PREFIX,
        apiKeyRequired = false
    ),
    ProviderPreset(
        id = "openai",
        name = "OpenAI",
        type = ProviderType.OPENAI,
        defaultBaseUrl = "https://api.openai.com/v1",
        apiKeyHint = "sk-"
    ),
    ProviderPreset(
        id = "anthropic",
        name = "Anthropic",
        type = ProviderType.ANTHROPIC,
        defaultBaseUrl = "https://api.anthropic.com",
        apiKeyHint = "sk-ant-"
    ),
    ProviderPreset(
        id = "gemini",
        name = "Google Gemini",
        type = ProviderType.GEMINI,
        defaultBaseUrl = "https://generativelanguage.googleapis.com",
        apiKeyHint = "AIza"
    ),
    ProviderPreset(
        id = "openrouter",
        name = "OpenRouter",
        type = ProviderType.COMPAT,
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        apiKeyHint = "sk-or-"
    ),
    ProviderPreset(
        id = "groq",
        name = "Groq",
        type = ProviderType.COMPAT,
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        apiKeyHint = "gsk_"
    ),
    ProviderPreset(
        id = "deepseek",
        name = "DeepSeek",
        type = ProviderType.COMPAT,
        defaultBaseUrl = "https://api.deepseek.com/v1",
        apiKeyHint = "sk-"
    ),
    ProviderPreset(
        id = "ollama",
        name = "Ollama (local)",
        type = ProviderType.COMPAT,
        defaultBaseUrl = "http://localhost:11434/v1",
        apiKeyHint = null,
        apiKeyRequired = false
    )
)

fun presetById(id: String?): ProviderPreset? =
    id?.let { key -> AllProviderPresets.firstOrNull { it.id == key } }