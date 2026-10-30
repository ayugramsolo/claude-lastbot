package com.scooter.control.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")
private val KEY_THEME = stringPreferencesKey("theme")
private val KEY_ANIM = booleanPreferencesKey("animations")
private val KEY_DISCLAIMER = booleanPreferencesKey("disclaimer_seen")
private val KEY_KAWAII = booleanPreferencesKey("kawaii_unlocked")

/** Тема, анимации, флаг «предупреждение прочитано» и секретный kawaii-режим. */
class SettingsStore(private val context: Context) {

    val themeFlow: Flow<String> = context.settingsDataStore.data.map { it[KEY_THEME] ?: "orange" }
    val animationsFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_ANIM] ?: true }
    val disclaimerSeenFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_DISCLAIMER] ?: false }
    val kawaiiFlow: Flow<Boolean> = context.settingsDataStore.data.map { it[KEY_KAWAII] ?: false }

    suspend fun setTheme(id: String) {
        context.settingsDataStore.edit { it[KEY_THEME] = id }
    }

    suspend fun setAnimations(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_ANIM] = enabled }
    }

    suspend fun setDisclaimerSeen() {
        context.settingsDataStore.edit { it[KEY_DISCLAIMER] = true }
    }

    suspend fun setKawaii(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_KAWAII] = enabled }
    }
}
