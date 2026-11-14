package com.moataz.motazalqmi.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

enum class AppThemeMode(val labelAr: String) {
    SYSTEM("تلقائي حسب النظام"),
    LIGHT("الوضع النهاري الورقي"),
    DARK("الوضع الليلي الأدبي")
}

private val LightColorScheme = lightColorScheme(
    primary = DeepOliveTeal,
    onPrimary = ParchmentSurface,
    primaryContainer = OliveTealContainer,
    onPrimaryContainer = DeepOliveTeal,
    secondary = AntiqueGold,
    onSecondary = ParchmentSurface,
    background = ParchmentBackground,
    onBackground = DeepInkText,
    surface = ParchmentSurface,
    onSurface = DeepInkText,
    surfaceVariant = ParchmentSurfaceVariant,
    onSurfaceVariant = MutedInkText,
    outline = BorderParchment
)

private val DarkColorScheme = darkColorScheme(
    primary = NightOliveTeal,
    onPrimary = NightBackground,
    primaryContainer = NightOliveContainer,
    onPrimaryContainer = NightInkText,
    secondary = NightGold,
    onSecondary = NightBackground,
    background = NightBackground,
    onBackground = NightInkText,
    surface = NightSurface,
    onSurface = NightInkText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMutedText,
    outline = NightBorder
)

@Composable
fun MotazAlalqamiTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = LiteraryTypography,
            content = content
        )
    }
}
