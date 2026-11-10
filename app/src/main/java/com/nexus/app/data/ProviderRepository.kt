package com.nexus.app.data

import com.nexus.app.data.crypto.SecretCipher
import com.nexus.app.data.db.ProviderDao
import com.nexus.app.data.db.ProviderEntity
import com.nexus.app.providers.ModelInfo
import com.nexus.app.providers.ProviderAdapter
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.ProviderRegistry
import com.nexus.app.providers.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class ProviderExportDto(
    val name: String,
    val type: String,
    val baseUrl: String,
    val models: List<String> = emptyList(),
    val enabled: Boolean = true,
    val apiKey: String = ""
)

class ProviderRepository(
    private val dao: ProviderDao,
    private val crypto: SecretCipher,
    private val registry: ProviderRegistry
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun observeConfigs(): Flow<List<ProviderConfig>> =
        dao.observeAll().map { list -> list.map { it.toConfig() } }

    suspend fun getConfigs(): List<ProviderConfig> =
        dao.observeAll().first().map { it.toConfig() }

    suspend fun findById(id: String): ProviderConfig? = dao.findById(id)?.toConfig()

    suspend fun save(config: ProviderConfig) {
        dao.upsert(config.toEntity())
    }

    suspend fun delete(id: String) = dao.deleteById(id)

    fun adapterFor(config: ProviderConfig): ProviderAdapter = registry.adapterFor(config)

    suspend fun testConnection(config: ProviderConfig): Result<Unit> =
        registry.adapterFor(config).testConnection()

    suspend fun fetchModels(config: ProviderConfig): List<ModelInfo> =
        registry.adapterFor(config).listModels()

    fun defaultBaseUrl(type: ProviderType): String = registry.defaultBaseUrl(type)

    suspend fun exportJson(includeKeys: Boolean = false): String {
        val rows = dao.observeAll().first()
        val dtos = rows.map { e ->
            ProviderExportDto(
                name = e.name,
                type = e.type,
                baseUrl = e.baseUrl,
                models = e.modelsCsv.split(",").filter { it.isNotBlank() },
                enabled = e.enabled,
                apiKey = if (includeKeys) crypto.decrypt(e.apiKeyEncrypted) else ""
            )
        }
        return json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ProviderExportDto.serializer()), dtos)
    }

    suspend fun importJson(raw: String) {
        val dtos = json.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(ProviderExportDto.serializer()),
            raw
        )
        dtos.forEach { dto ->
            val type = ProviderType.entries.find { it.name == dto.type } ?: ProviderType.COMPAT
            val config = ProviderConfig(
                id = UUID.randomUUID().toString(),
                name = dto.name,
                type = type,
                baseUrl = dto.baseUrl,
                apiKey = dto.apiKey,
                models = dto.models,
                enabled = dto.enabled
            )
            dao.upsert(config.toEntity())
        }
    }

    private fun ProviderEntity.toConfig(): ProviderConfig = ProviderConfig(
        id = id,
        name = name,
        type = ProviderType.entries.find { it.name == type } ?: ProviderType.COMPAT,
        baseUrl = baseUrl,
        apiKey = runCatching { crypto.decrypt(apiKeyEncrypted) }.getOrDefault(""),
        models = modelsCsv.split(",").filter { it.isNotBlank() },
        enabled = enabled,
        temperature = temperature,
        maxTokens = maxTokens,
        reasoningEffort = reasoningEffort
    )

    private fun ProviderConfig.toEntity(): ProviderEntity = ProviderEntity(
        id = id,
        name = name,
        type = type.name,
        baseUrl = baseUrl,
        apiKeyEncrypted = crypto.encrypt(apiKey),
        modelsCsv = models.joinToString(","),
        enabled = enabled,
        temperature = temperature,
        maxTokens = maxTokens,
        reasoningEffort = reasoningEffort
    )
}