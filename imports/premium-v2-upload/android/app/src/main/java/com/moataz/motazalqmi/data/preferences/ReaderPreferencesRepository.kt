package com.moataz.motazalqmi.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.moataz.motazalqmi.domain.model.ReaderSettings
import com.moataz.motazalqmi.ui.theme.AppThemeMode
import com.moataz.motazalqmi.ui.theme.ArabicFontOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "motaz_reader_prefs")

@Singleton
class ReaderPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val FONT = stringPreferencesKey("font_option")
        val FONT_SIZE = floatPreferencesKey("font_size_sp")
        val LINE_HEIGHT = floatPreferencesKey("line_height_mult")
        val NIGHT_READING = booleanPreferencesKey("night_reading")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only_images")
        val LAST_SYNC = stringPreferencesKey("last_sync_iso")
    }

    val settingsFlow: Flow<ReaderSettings> = context.dataStore.data.map { prefs ->
        ReaderSettings(
            themeMode = prefs[Keys.THEME]?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() } ?: AppThemeMode.SYSTEM,
            fontOption = prefs[Keys.FONT]?.let { runCatching { ArabicFontOption.valueOf(it) }.getOrNull() } ?: ArabicFontOption.NOTO_NASKH,
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: 18f,
            lineHeightMultiplier = prefs[Keys.LINE_HEIGHT] ?: 1.75f,
            nightReadingMode = prefs[Keys.NIGHT_READING] ?: false,
            wifiOnlyImages = prefs[Keys.WIFI_ONLY] ?: false,
            lastSyncIso = prefs[Keys.LAST_SYNC]
        )
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setFontOption(option: ArabicFontOption) {
        context.dataStore.edit { it[Keys.FONT] = option.name }
    }

    suspend fun setFontSize(sizeSp: Float) {
        context.dataStore.edit { it[Keys.FONT_SIZE] = sizeSp.coerceIn(14f, 28f) }
    }

    suspend fun setLineHeight(multiplier: Float) {
        context.dataStore.edit { it[Keys.LINE_HEIGHT] = multiplier.coerceIn(1.4f, 2.3f) }
    }

    suspend fun setNightReading(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NIGHT_READING] = enabled }
    }

    suspend fun setWifiOnlyImages(enabled: Boolean) {
        context.dataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun setLastSyncIso(iso: String) {
        context.dataStore.edit { it[Keys.LAST_SYNC] = iso }
    }
}
