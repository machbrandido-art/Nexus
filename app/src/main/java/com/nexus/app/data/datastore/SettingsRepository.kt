package com.nexus.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Density { COMPACT, COMFORTABLE }

/** Default seed: warm terracotta. Matches the app's identity when dynamic color is off. */
const val DEFAULT_SEED_COLOR_ARGB: Long = 0xFFC96A48L

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_THEME]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    /**
     * Dynamic color is ON by default. On API 31+ this uses the wallpaper palette;
     * on older versions it silently falls back to the seed-color scheme.
     */
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DYNAMIC] ?: true
    }

    val seedColorArgb: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_SEED_COLOR] ?: DEFAULT_SEED_COLOR_ARGB
    }

    val density: Flow<Density> = context.dataStore.data.map { prefs ->
        if (prefs[KEY_DENSITY] == "COMPACT") Density.COMPACT else Density.COMFORTABLE
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[KEY_THEME] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DYNAMIC] = enabled }
    }

    suspend fun setSeedColor(argb: Long) {
        context.dataStore.edit { it[KEY_SEED_COLOR] = argb }
    }

    suspend fun setDensity(density: Density) {
        context.dataStore.edit { it[KEY_DENSITY] = density.name }
    }

    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
        private val KEY_SEED_COLOR = longPreferencesKey("seed_color_argb")
        private val KEY_DENSITY = stringPreferencesKey("density")
    }
}