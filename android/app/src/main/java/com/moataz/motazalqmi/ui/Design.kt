package com.moataz.motazalqmi.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.moataz.motazalqmi.R

val Ink = Color(0xFF30271F)
val Gold = Color(0xFF96702E)
val Teal = Color(0xFF285D57)
val Paper = Color(0xFFFAF4E9)
val Plex = FontFamily(Font(R.font.plex))
val Naskh = FontFamily(Font(R.font.naskh))
val Amiri = FontFamily(Font(R.font.amiri))
@Composable fun BlogTheme(theme: String, content: @Composable () -> Unit) {
 val dark = theme == "dark" || (theme == "system" && isSystemInDarkTheme())
 val colors = if (dark) darkColorScheme(primary = Color(0xFFD6BC82), secondary = Color(0xFF9AC6B9),
  background = Color(0xFF191C19), surface = Color(0xFF202620), onSurface = Color(0xFFF2E9D7), onBackground = Color(0xFFF2E9D7))
 else lightColorScheme(primary = Teal, secondary = Gold, background = Paper, surface = Color(0xFFFFFBF2), onSurface = Ink, onBackground = Ink)
 val base = Typography()
 MaterialTheme(colorScheme = colors, typography = base.copy(
  bodyLarge = base.bodyLarge.copy(fontFamily = Plex), bodyMedium = base.bodyMedium.copy(fontFamily = Plex),
  titleLarge = base.titleLarge.copy(fontFamily = Amiri), titleMedium = base.titleMedium.copy(fontFamily = Plex),
  labelLarge = base.labelLarge.copy(fontFamily = Plex), labelMedium = base.labelMedium.copy(fontFamily = Plex),
  headlineLarge = base.headlineLarge.copy(fontFamily = Amiri), headlineMedium = base.headlineMedium.copy(fontFamily = Amiri)), content = content)
}
