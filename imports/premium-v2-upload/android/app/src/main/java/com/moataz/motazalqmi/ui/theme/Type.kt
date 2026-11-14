package com.moataz.motazalqmi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

enum class ArabicFontOption(val displayNameAr: String) {
    AMIRI("الأميري — أدبي وشعري"),
    NOTO_NASKH("نسخ نوتو — مريح للمقالات"),
    IBM_PLEX_ARABIC("آي بي إم بلكس — عصري واضح")
}

// Note: Bundled TTFs in res/font/ or Google Fonts provider can be attached here.
// Using Serif/Sans fallbacks ensures zero-crash rendering even offline.
val AmiriFamily = FontFamily.Serif
val NotoNaskhFamily = FontFamily.Serif
val IbmPlexArabicFamily = FontFamily.SansSerif

fun fontFamilyFor(option: ArabicFontOption): FontFamily = when (option) {
    ArabicFontOption.AMIRI -> AmiriFamily
    ArabicFontOption.NOTO_NASKH -> NotoNaskhFamily
    ArabicFontOption.IBM_PLEX_ARABIC -> IbmPlexArabicFamily
}

val LiteraryTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 40.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 36.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = AmiriFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoNaskhFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 30.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoNaskhFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 26.sp
    ),
    labelMedium = TextStyle(
        fontFamily = IbmPlexArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)
