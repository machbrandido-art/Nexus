package com.nexus.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexus.app.data.datastore.DEFAULT_SEED_COLOR_ARGB
import com.nexus.app.data.datastore.Density
import com.nexus.app.data.datastore.SettingsRepository
import com.nexus.app.data.datastore.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> =
        settings.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val dynamicColor: StateFlow<Boolean> =
        settings.dynamicColor.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val seedColorArgb: StateFlow<Long> =
        settings.seedColorArgb.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            DEFAULT_SEED_COLOR_ARGB
        )

    val density: StateFlow<Density> =
        settings.density.stateIn(viewModelScope, SharingStarted.Eagerly, Density.COMFORTABLE)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settings.setDynamicColor(enabled) }
    }

    fun setSeedColor(argb: Long) {
        viewModelScope.launch { settings.setSeedColor(argb) }
    }

    fun setDensity(density: Density) {
        viewModelScope.launch { settings.setDensity(density) }
    }
}