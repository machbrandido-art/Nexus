package com.nexus.app.providers.openai

import com.nexus.app.providers.ModelInfo
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.compat.OpenAiCompatibleAdapter
import okhttp3.OkHttpClient

class OpenAiAdapter(
    client: OkHttpClient,
    config: ProviderConfig
) : OpenAiCompatibleAdapter(client, config) {

    override val fallbackModels: List<ModelInfo> = listOf(
        ModelInfo("gpt-4o", "GPT-4o", supportsVision = true, supportsTools = true),
        ModelInfo("gpt-4o-mini", "GPT-4o mini", supportsVision = true, supportsTools = true),
        ModelInfo("gpt-4-turbo", "GPT-4 Turbo", supportsVision = true, supportsTools = true),
        ModelInfo("o1", "o1", supportsTools = true),
        ModelInfo("o1-mini", "o1 mini"),
        ModelInfo("o3-mini", "o3-mini", supportsTools = true)
    )
}