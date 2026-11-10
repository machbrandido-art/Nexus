package com.nexus.app.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.ConversationRepository
import com.nexus.app.data.ProviderRepository
import com.nexus.app.data.datastore.SettingsRepository
import com.nexus.app.data.db.MessageEntity
import com.nexus.app.modes.Mode
import com.nexus.app.modes.ModeRepository
import com.nexus.app.providers.ChatEvent
import com.nexus.app.providers.ChatMessage
import com.nexus.app.providers.ChatRequest
import com.nexus.app.providers.ProviderConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(
    val id: String,
    val role: ChatMessage.Role,
    val content: String,
    val reasoning: String?
)

data class ChatUiState(
    val conversationId: String? = null,
    val title: String = "New chat",
    val messages: List<UiMessage> = emptyList(),
    val streamingText: String = "",
    val streamingReasoning: String = "",
    val isStreaming: Boolean = false,
    val input: String = "",
    val error: String? = null,
    val providers: List<ProviderConfig> = emptyList(),
    val selectedProviderId: String? = null,
    val selectedModel: String? = null,
    val modeId: String = "chat"
)

class ChatViewModel(
    private val conversationRepo: ConversationRepository,
    private val providerRepo: ProviderRepository,
    private val modeRepo: ModeRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var observeJob: Job? = null

    init {
        viewModelScope.launch {
            providerRepo.observeConfigs().collect { configs ->
                _uiState.update { state ->
                    val enabled = configs.filter { it.enabled }
                    val stillValid = enabled.any { it.id == state.selectedProviderId }
                    val current = if (stillValid) state.selectedProviderId else enabled.firstOrNull()?.id
                    val currentModels = enabled.find { it.id == current }?.models.orEmpty()
                    val model = when {
                        stillValid && state.selectedModel in currentModels -> state.selectedModel
                        else -> currentModels.firstOrNull()
                    }
                    state.copy(
                        providers = enabled,
                        selectedProviderId = current,
                        selectedModel = model
                    )
                }
            }
        }
        viewModelScope.launch {
            modeRepo.currentModeId.collect { id ->
                _uiState.update { it.copy(modeId = id) }
            }
        }
    }

    fun updateInput(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun selectProviderModel(providerId: String, model: String) {
        _uiState.update { it.copy(selectedProviderId = providerId, selectedModel = model) }
        val convId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            val s = _uiState.value
            conversationRepo.updateConversationMeta(convId, providerId, model, s.modeId)
        }
    }

    fun switchMode(modeId: String) {
        modeRepo.setCurrentMode(modeId)
        val convId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            val s = _uiState.value
            conversationRepo.updateConversationMeta(convId, s.selectedProviderId, s.selectedModel, modeId)
        }
    }

    fun newChat() {
        streamJob?.cancel()
        observeJob?.cancel()
        _uiState.update {
            it.copy(
                conversationId = null,
                title = "New chat",
                messages = emptyList(),
                streamingText = "",
                streamingReasoning = "",
                isStreaming = false,
                input = "",
                error = null
            )
        }
    }

    fun cancelStreaming() {
        streamJob?.cancel()
        streamJob = null
    }

    fun send() {
        val state = _uiState.value
        val text = state.input.trim()
        if (text.isEmpty() || state.isStreaming) return
        val providerConfig = state.providers.find { it.id == state.selectedProviderId }
        val model = state.selectedModel
        if (providerConfig == null || model.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Pick a provider and model first") }
            return
        }
        _uiState.update { it.copy(input = "", error = null) }

        viewModelScope.launch {
            val convId = state.conversationId ?: run {
                val id = conversationRepo.createConversation(
                    title = text.take(48).ifBlank { "New chat" },
                    providerId = providerConfig.id,
                    model = model,
                    mode = state.modeId
                )
                _uiState.update { it.copy(conversationId = id) }
                observeConversation(id)
                id
            }
            conversationRepo.appendMessage(convId, ChatMessage.Role.USER.name, text)
            startStreaming(convId, providerConfig, model, text)
        }
    }

    private fun observeConversation(conversationId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            conversationRepo.observeMessages(conversationId).collect { list ->
                _uiState.update { state ->
                    val msgs = list.map { it.toUi() }
                    val last = msgs.lastOrNull()
                    val clearStreaming = !state.isStreaming &&
                        last?.role == ChatMessage.Role.ASSISTANT &&
                        last.content == state.streamingText
                    state.copy(
                        messages = msgs,
                        streamingText = if (clearStreaming) "" else state.streamingText,
                        streamingReasoning = if (clearStreaming) "" else state.streamingReasoning
                    )
                }
            }
        }
        viewModelScope.launch {
            conversationRepo.observeConversation(conversationId).collect { c ->
                if (c != null) _uiState.update { it.copy(title = c.title) }
            }
        }
    }

    private fun startStreaming(
        conversationId: String,
        provider: ProviderConfig,
        model: String,
        userText: String
    ) {
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _uiState.update {
                it.copy(isStreaming = true, streamingText = "", streamingReasoning = "")
            }
            val mode: Mode = modeRepo.findById(_uiState.value.modeId)
            val history = _uiState.value.messages
                .filter { it.role == ChatMessage.Role.USER || it.role == ChatMessage.Role.ASSISTANT }
                .map { ChatMessage(it.role, it.content) }
                .let { list ->
                    if (list.lastOrNull()?.content == userText) list
                    else list + ChatMessage(ChatMessage.Role.USER, userText)
                }
            val request = ChatRequest(
                model = model,
                messages = history,
                systemPrompt = mode.systemPrompt,
                temperature = provider.temperature ?: mode.temperature,
                maxTokens = provider.maxTokens,
                reasoningEffort = provider.reasoningEffort
            )
            try {
                providerRepo.adapterFor(provider).streamChat(request).collect { event ->
                    when (event) {
                        is ChatEvent.Token -> _uiState.update { s ->
                            s.copy(streamingText = s.streamingText + event.text)
                        }
                        is ChatEvent.Reasoning -> _uiState.update { s ->
                            s.copy(streamingReasoning = s.streamingReasoning + event.text)
                        }
                        is ChatEvent.Error -> _uiState.update { s ->
                            s.copy(error = event.message)
                        }
                        is ChatEvent.Citation, ChatEvent.Done -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Stream failed") }
            } finally {
                val snap = _uiState.value
                if (snap.streamingText.isNotBlank()) {
                    conversationRepo.appendMessage(
                        conversationId,
                        ChatMessage.Role.ASSISTANT.name,
                        snap.streamingText,
                        reasoning = snap.streamingReasoning.takeIf { it.isNotBlank() }
                    )
                }
                _uiState.update { it.copy(isStreaming = false) }
            }
        }
    }

    private fun MessageEntity.toUi(): UiMessage = UiMessage(
        id = id,
        role = when (role) {
            "USER" -> ChatMessage.Role.USER
            "ASSISTANT" -> ChatMessage.Role.ASSISTANT
            else -> ChatMessage.Role.SYSTEM
        },
        content = content,
        reasoning = reasoning
    )
}