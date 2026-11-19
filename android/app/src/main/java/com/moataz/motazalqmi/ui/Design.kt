package com.moataz.motazalqmi.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.moataz.motazalqmi.R

val Ink = Color(0xFF202625)
val Gold = Color(0xFFA67E39)
val Teal = Color(0xFF164541)
val Paper = Color(0xFFF7F3EB)
val Plex = FontFamily(Font(R.font.plex))
val Naskh = FontFamily(Font(R.font.naskh))
val Amiri = FontFamily(Font(R.font.amiri))
@Composable fun BlogTheme(theme: String, content: @Composable () -> Unit) {
 val dark = theme == "dark" || (theme == "system" && isSystemInDarkTheme())
 val colors = if (dark) darkColorScheme(primary = Color(0xFFD6BC82), secondary = Color(0xFF9AC6B9),
  background = Color(0xFF191C19), surface = Color(0xFF202620), onSurface = Color(0xFFF2E9D7), onBackground = Color(0xFFF2E9D7))
 else lightColorScheme(primary = Teal, secondary = Color(0xFF795A28), background = Paper, surface = Color(0xFFFFFCF5), primaryContainer = Color(0xFFE4EFEA), surfaceVariant = Color(0xFFEFE7DA), onSurface = Ink, onBackground = Ink)
 val base = Typography()
 MaterialTheme(colorScheme = colors, typography = base.copy(
  bodySmall = base.bodySmall.copy(fontFamily = Plex), labelSmall = base.labelSmall.copy(fontFamily = Plex),
  bodyLarge = base.bodyLarge.copy(fontFamily = Plex), bodyMedium = base.bodyMedium.copy(fontFamily = Plex),
  titleLarge = base.titleLarge.copy(fontFamily = Amiri), titleMedium = base.titleMedium.copy(fontFamily = Plex),
  labelLarge = base.labelLarge.copy(fontFamily = Plex), labelMedium = base.labelMedium.copy(fontFamily = Plex),
  headlineLarge = base.headlineLarge.copy(fontFamily = Amiri), headlineMedium = base.headlineMedium.copy(fontFamily = Amiri)), content = content)
}
