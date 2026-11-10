package com.nexus.app.features.kanban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.db.KanbanCardEntity
import com.nexus.app.data.db.KanbanDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class KanbanUiState(
    val cards: List<KanbanCardEntity> = emptyList()
)

class KanbanViewModel(
    private val dao: KanbanDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(KanbanUiState())
    val uiState: StateFlow<KanbanUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            dao.observeAll().collect { cards ->
                _uiState.update { it.copy(cards = cards) }
            }
        }
    }

    fun addCard(title: String, notes: String, status: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val nextOrder = (_uiState.value.cards
                .filter { it.status == status }
                .maxOfOrNull { it.sortOrder } ?: 0) + 1
            dao.upsert(
                KanbanCardEntity(
                    id = UUID.randomUUID().toString(),
                    title = title.trim(),
                    notes = notes.trim(),
                    status = status,
                    sortOrder = nextOrder,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun moveCard(id: String, targetStatus: String) {
        viewModelScope.launch {
            val nextOrder = (_uiState.value.cards
                .filter { it.status == targetStatus && it.id != id }
                .maxOfOrNull { it.sortOrder } ?: 0) + 1
            dao.move(id, targetStatus, nextOrder)
        }
    }

    fun deleteCard(id: String) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}