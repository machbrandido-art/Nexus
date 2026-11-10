package com.nexus.app.providers

import com.nexus.app.providers.anthropic.AnthropicAdapter
import com.nexus.app.providers.compat.OpenAiCompatibleAdapter
import com.nexus.app.providers.gemini.GeminiAdapter
import com.nexus.app.providers.openai.OpenAiAdapter
import okhttp3.OkHttpClient

class ProviderRegistry(private val httpClient: OkHttpClient) {

    fun adapterFor(config: ProviderConfig): ProviderAdapter = when (config.type) {
        ProviderType.OPENAI -> OpenAiAdapter(httpClient, config)
        ProviderType.ANTHROPIC -> AnthropicAdapter(httpClient, config)
        ProviderType.GEMINI -> GeminiAdapter(httpClient, config)
        ProviderType.COMPAT -> OpenAiCompatibleAdapter(httpClient, config)
    }

    fun defaultBaseUrl(type: ProviderType): String = when (type) {
        ProviderType.OPENAI -> "https://api.openai.com/v1"
        ProviderType.ANTHROPIC -> "https://api.anthropic.com"
        ProviderType.GEMINI -> "https://generativelanguage.googleapis.com"
        ProviderType.COMPAT -> "https://openrouter.ai/api/v1"
    }
}