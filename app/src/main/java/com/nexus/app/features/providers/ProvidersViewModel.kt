package com.nexus.app.features.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.ProviderRepository
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.ProviderRegistry
import com.nexus.app.providers.ProviderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ProviderForm(
    val id: String? = null,
    val name: String = "",
    val type: ProviderType = ProviderType.OPENAI,
    val baseUrl: String = "",
    val apiKey: String = "",
    val modelsCsv: String = "",
    val enabled: Boolean = true,
    val temperature: String = "",
    val maxTokens: String = ""
)

data class ProvidersUiState(
    val providers: List<ProviderConfig> = emptyList(),
    val editing: ProviderForm? = null,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val isFetchingModels: Boolean = false,
    val error: String? = null,
    val info: String? = null
)

class ProvidersViewModel(
    private val providerRepo: ProviderRepository,
    @Suppress("unused") private val registry: ProviderRegistry
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProvidersUiState())
    val uiState: StateFlow<ProvidersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            providerRepo.observeConfigs().collect { configs ->
                _uiState.update { it.copy(providers = configs) }
            }
        }
    }

    fun startAdd() {
        _uiState.update {
            it.copy(
                editing = ProviderForm(
                    type = ProviderType.OPENAI,
                    baseUrl = providerRepo.defaultBaseUrl(ProviderType.OPENAI)
                ),
                error = null,
                info = null,
                testResult = null
            )
        }
    }

    fun startEdit(provider: ProviderConfig) {
        _uiState.update {
            it.copy(
                editing = ProviderForm(
                    id = provider.id,
                    name = provider.name,
                    type = provider.type,
                    baseUrl = provider.baseUrl,
                    apiKey = provider.apiKey,
                    modelsCsv = provider.models.joinToString(","),
                    enabled = provider.enabled,
                    temperature = provider.temperature?.toString() ?: "",
                    maxTokens = provider.maxTokens?.toString() ?: ""
                ),
                error = null,
                info = null,
                testResult = null
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { it.copy(editing = null, testResult = null, error = null, info = null) }
    }

    fun updateForm(transform: (ProviderForm) -> ProviderForm) {
        _uiState.update { state ->
            val form = state.editing ?: return@update state
            state.copy(editing = transform(form))
        }
    }

    fun onTypeChange(type: ProviderType) {
        _uiState.update { state ->
            val f = state.editing ?: return@update state
            state.copy(
                editing = f.copy(
                    type = type,
                    baseUrl = providerRepo.defaultBaseUrl(type)
                )
            )
        }
    }

    fun save() {
        val form = _uiState.value.editing ?: return
        if (form.name.isBlank() || form.baseUrl.isBlank()) {
            _uiState.update { it.copy(error = "Name and base URL are required") }
            return
        }
        viewModelScope.launch {
            val config = ProviderConfig(
                id = form.id ?: UUID.randomUUID().toString(),
                name = form.name.trim(),
                type = form.type,
                baseUrl = form.baseUrl.trim(),
                apiKey = form.apiKey.trim(),
                models = form.modelsCsv.split(",").map { it.trim() }.filter { it.isNotBlank() },
                enabled = form.enabled,
                temperature = form.temperature.toFloatOrNull(),
                maxTokens = form.maxTokens.toIntOrNull()
            )
            providerRepo.save(config)
            _uiState.update { it.copy(editing = null) }
        }
    }

    fun delete(provider: ProviderConfig) {
        viewModelScope.launch { providerRepo.delete(provider.id) }
    }

    fun duplicate(provider: ProviderConfig) {
        viewModelScope.launch {
            val copy = provider.copy(
                id = UUID.randomUUID().toString(),
                name = provider.name + " (copy)"
            )
            providerRepo.save(copy)
        }
    }

    fun testConnection() {
        val form = _uiState.value.editing ?: return
        if (form.baseUrl.isBlank()) {
            _uiState.update { it.copy(testResult = "Add a base URL first") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            val config = ProviderConfig(
                id = form.id ?: "test",
                name = form.name.ifBlank { "test" },
                type = form.type,
                baseUrl = form.baseUrl,
                apiKey = form.apiKey,
                models = emptyList(),
                enabled = true
            )
            val result = providerRepo.testConnection(config)
            _uiState.update {
                it.copy(
                    isTesting = false,
                    testResult = result.fold(
                        onSuccess = { "Connection works" },
                        onFailure = { t -> "Failed: ${t.message ?: "unknown"}" }
                    )
                )
            }
        }
    }

    fun fetchModels() {
        val form = _uiState.value.editing ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingModels = true, info = null, error = null) }
            val config = ProviderConfig(
                id = form.id ?: "fetch",
                name = form.name.ifBlank { "fetch" },
                type = form.type,
                baseUrl = form.baseUrl,
                apiKey = form.apiKey,
                models = emptyList(),
                enabled = true
            )
            try {
                val models = providerRepo.fetchModels(config)
                _uiState.update { state ->
                    val f = state.editing ?: return@update state
                    state.copy(
                        isFetchingModels = false,
                        editing = f.copy(modelsCsv = models.joinToString(",") { it.id }),
                        info = "Fetched ${models.size} models"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isFetchingModels = false, error = "Fetch failed: ${e.message ?: "unknown"}")
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, info = null, testResult = null) }
    }
}