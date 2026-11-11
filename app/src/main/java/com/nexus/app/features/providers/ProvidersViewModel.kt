package com.nexus.app.features.providers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.ProviderRepository
import com.nexus.app.providers.ModelInfo
import com.nexus.app.providers.ProviderConfig
import com.nexus.app.providers.ProviderRegistry
import com.nexus.app.providers.ProviderType
import com.nexus.app.providers.compat.NimPreset
import com.nexus.app.providers.compat.ProviderPreset
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
    val maxTokens: String = "",
    val presetId: String? = null
)

data class ProvidersUiState(
    val providers: List<ProviderConfig> = emptyList(),
    val editing: ProviderForm? = null,
    val isTesting: Boolean = false,
    val testResult: String? = null,
    val isFetchingModels: Boolean = false,
    val error: String? = null,
    val info: String? = null,
    val fetchedModels: List<ModelInfo> = emptyList(),
    val showModelPicker: Boolean = false,
    val selectedModelIds: Set<String> = emptySet()
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
                testResult = null,
                fetchedModels = emptyList(),
                selectedModelIds = emptySet()
            )
        }
    }

    fun startAddPreset(preset: ProviderPreset) {
        _uiState.update {
            it.copy(
                editing = ProviderForm(
                    name = preset.name,
                    type = preset.type,
                    baseUrl = preset.defaultBaseUrl,
                    presetId = preset.id
                ),
                error = null,
                info = null,
                testResult = null,
                fetchedModels = emptyList(),
                selectedModelIds = emptySet()
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
                    maxTokens = provider.maxTokens?.toString() ?: "",
                    presetId = provider.presetId
                ),
                error = null,
                info = null,
                testResult = null,
                fetchedModels = emptyList(),
                selectedModelIds = emptySet()
            )
        }
    }

    fun cancelEdit() {
        _uiState.update {
            it.copy(
                editing = null,
                testResult = null,
                error = null,
                info = null,
                fetchedModels = emptyList(),
                selectedModelIds = emptySet(),
                showModelPicker = false
            )
        }
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
        val preset = com.nexus.app.providers.compat.presetById(form.presetId)
        if (preset != null && preset.apiKeyRequired && form.apiKey.isBlank()) {
            _uiState.update { it.copy(error = "API key is required for this provider") }
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
                maxTokens = form.maxTokens.toIntOrNull(),
                presetId = form.presetId
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
                enabled = true,
                presetId = form.presetId
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
                enabled = true,
                presetId = form.presetId
            )
            try {
                val models = providerRepo.fetchModels(config)
                _uiState.update { state ->
                    val f = state.editing ?: return@update state
                    val csv = models.joinToString(",") { it.id }
                    state.copy(
                        isFetchingModels = false,
                        editing = f.copy(modelsCsv = csv),
                        fetchedModels = models,
                        selectedModelIds = models.map { it.id }.toSet(),
                        info = "Fetched ${models.size} models"
                    )
                }
                val isNim = _uiState.value.editing?.presetId == NimPreset.ID
                if (isNim && models.isNotEmpty()) {
                    _uiState.update { it.copy(showModelPicker = true) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isFetchingModels = false,
                        error = "Fetch failed: ${e.message ?: "unknown"}"
                    )
                }
            }
        }
    }

    fun openModelPicker() {
        if (_uiState.value.fetchedModels.isEmpty()) return
        _uiState.update { it.copy(showModelPicker = true) }
    }

    fun closeModelPicker() {
        _uiState.update { it.copy(showModelPicker = false) }
    }

    fun toggleModel(id: String) {
        _uiState.update { state ->
            val newSet = if (id in state.selectedModelIds) {
                state.selectedModelIds - id
            } else {
                state.selectedModelIds + id
            }
            state.copy(selectedModelIds = newSet)
        }
    }

    fun applyModelSelection() {
        _uiState.update { state ->
            val f = state.editing ?: return@update state
            val csv = state.selectedModelIds.joinToString(",")
            state.copy(
                editing = f.copy(modelsCsv = csv),
                showModelPicker = false
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, info = null, testResult = null) }
    }
}