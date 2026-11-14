package com.moataz.motazalqmi.domain.model

import com.moataz.motazalqmi.ui.theme.AppThemeMode
import com.moataz.motazalqmi.ui.theme.ArabicFontOption

data class Category(
    val id: String,
    val slug: String,
    val nameAr: String,
    val description: String = "",
    val sortOrder: Int = 0,
    val articleCount: Int = 0
)

data class Article(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String,
    val contentMarkdown: String,
    val coverImageUrl: String?,
    val authorName: String,
    val categoryId: String,
    val categoryName: String,
    val tags: List<String>,
    val readingTimeMinutes: Int,
    val publishedAt: String,
    val updatedAt: String,
    val isFeatured: Boolean = false,
    val isBookmarked: Boolean = false,
    val lastReadPosition: Int = 0,
    val readProgress: Float = 0f,
    val lastReadAt: Long = 0L
)

data class Quote(
    val id: String,
    val text: String,
    val author: String,
    val source: String?,
    val categoryId: String?,
    val updatedAt: String,
    val isFavorite: Boolean = false
)

data class ReaderSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val fontOption: ArabicFontOption = ArabicFontOption.NOTO_NASKH,
    val fontSizeSp: Float = 18f,
    val lineHeightMultiplier: Float = 1.75f,
    val nightReadingMode: Boolean = false,
    val wifiOnlyImages: Boolean = false,
    val lastSyncIso: String? = null
)

sealed interface SyncResult {
    data class Success(val syncedAtIso: String, val updatedCount: Int, val deletedCount: Int) : SyncResult
    data class Error(val message: String) : SyncResult
}
