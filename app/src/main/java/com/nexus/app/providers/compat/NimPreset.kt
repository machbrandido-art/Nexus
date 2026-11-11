package com.nexus.app.providers.compat

/**
 * NVIDIA NIM is served through the OpenAI-compatible adapter — this file holds
 * only the preset marker, defaults, and preset-specific behaviours (error copy,
 * vendor grouping for the model picker).
 */
object NimPreset {
    const val ID = "nim"
    const val NAME = "NVIDIA NIM"
    const val CLOUD_BASE_URL = "https://integrate.api.nvidia.com/v1"
    const val SELF_HOSTED_HINT = "http://<host>:8000/v1"
    const val API_KEY_PREFIX = "nvapi-"

    fun isCloudEndpoint(baseUrl: String): Boolean =
        baseUrl.trimEnd('/').startsWith("https://integrate.api.nvidia.com")

    /**
     * Group model IDs by their vendor prefix so the picker can render sections.
     * IDs look like `meta/llama-3.1-70b-instruct` or `nvidia/nemotron-...`; anything
     * without a slash lands under `other`.
     */
    fun groupModelsByVendor(models: List<String>): Map<String, List<String>> {
        val groups = models.groupBy { model ->
            val slash = model.indexOf('/')
            if (slash > 0) model.substring(0, slash) else "other"
        }
        return groups.toSortedMap()
    }

    /**
     * Returns the preset-specific error message for a given HTTP status code,
     * or `null` if this status does not have a NIM-specific message.
     */
    fun errorMessageForStatus(code: Int): String? = when (code) {
        401, 403 -> "Invalid NVIDIA key"
        429 -> "Rate limit reached (free tier credits may be exhausted)"
        404 -> "Model not available on this endpoint"
        else -> null
    }
}