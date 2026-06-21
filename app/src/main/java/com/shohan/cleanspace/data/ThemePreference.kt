package com.shohan.cleanspace.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shohan.cleanspace.data.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "cleanspace_settings")

class ThemePreference(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme_mode")
    private val thresholdKey = longPreferencesKey("large_file_threshold_mb")

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[themeKey]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val largeFileThresholdMB: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[thresholdKey] ?: 50L
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[themeKey] = mode.name }
    }

    suspend fun setLargeFileThresholdMB(value: Long) {
        context.dataStore.edit { prefs -> prefs[thresholdKey] = value }
    }
}
