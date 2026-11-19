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
 val spacing: Float = 1.8f, val font: String = "naskh", val wifiImages: Boolean = false,
 val paragraphSpacing: Float = 1.2f, val textWidth: String = "regular", val alignment: String = "start",
 val useArticleSize: Boolean = false)
@Singleton class Settings @Inject constructor(@ApplicationContext private val context: Context) {
 private val theme = stringPreferencesKey("theme")
 private val size = floatPreferencesKey("size")
 private val spacing = floatPreferencesKey("spacing")
 private val font = stringPreferencesKey("font")
 private val wifi = booleanPreferencesKey("wifi_images")
 private val paragraph = floatPreferencesKey("paragraph_spacing")
 private val width = stringPreferencesKey("text_width")
 private val alignment = stringPreferencesKey("text_alignment")
 private val articleSize = booleanPreferencesKey("article_size")
 val flow = context.readerSettings.data.map {
  ReadingSettings(theme = it[theme] ?: "system", size = it[size] ?: 21f, spacing = it[spacing] ?: 1.8f,
   font = it[font] ?: "naskh", wifiImages = it[wifi] ?: false, paragraphSpacing = it[paragraph] ?: 1.2f,
   textWidth = it[width] ?: "regular", alignment = it[alignment] ?: "start", useArticleSize = it[articleSize] ?: false)
 }
 suspend fun update(value: ReadingSettings) { context.readerSettings.edit {
  it[theme] = value.theme; it[size] = value.size.coerceIn(16f, 34f)
  it[spacing] = value.spacing.coerceIn(1.3f, 2.5f); it[font] = value.font; it[wifi] = value.wifiImages
  it[paragraph] = value.paragraphSpacing.coerceIn(.6f, 1.8f)
  it[width] = value.textWidth
  it[alignment] = value.alignment
  it[articleSize] = value.useArticleSize
 } }
}
