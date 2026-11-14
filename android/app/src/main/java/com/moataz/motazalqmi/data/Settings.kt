package com.moataz.motazalqmi.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.readerSettings by preferencesDataStore("reader_v2")
data class ReadingSettings(val theme: String = "system", val size: Float = 21f,
 val spacing: Float = 1.8f, val font: String = "naskh", val wifiImages: Boolean = true)
@Singleton class Settings @Inject constructor(@ApplicationContext private val context: Context) {
 private val theme = stringPreferencesKey("theme")
 private val size = floatPreferencesKey("size")
 private val spacing = floatPreferencesKey("spacing")
 private val font = stringPreferencesKey("font")
 private val wifi = booleanPreferencesKey("wifi_images")
 val flow = context.readerSettings.data.map { ReadingSettings(it[theme] ?: "system", it[size] ?: 21f, it[spacing] ?: 1.8f, it[font] ?: "naskh", it[wifi] ?: true) }
 suspend fun update(value: ReadingSettings) { context.readerSettings.edit {
  it[theme] = value.theme; it[size] = value.size.coerceIn(16f, 34f)
  it[spacing] = value.spacing.coerceIn(1.3f, 2.4f); it[font] = value.font; it[wifi] = value.wifiImages
 } }
}
