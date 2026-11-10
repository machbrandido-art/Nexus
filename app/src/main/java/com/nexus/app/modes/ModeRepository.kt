package com.nexus.app.modes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ModeRepository {

    private val _currentModeId = MutableStateFlow("chat")
    val currentModeId: StateFlow<String> = _currentModeId.asStateFlow()

    fun setCurrentMode(id: String) {
        if (PresetModes.any { it.id == id }) _currentModeId.value = id
    }

    fun all(): List<Mode> = PresetModes

    fun findById(id: String): Mode =
        PresetModes.firstOrNull { it.id == id } ?: PresetModes.first()
}