package com.moataz.motazalqmi.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val nameAr: String,
    val description: String,
    val sortOrder: Int,
    val articleCount: Int
)

@Entity(
    tableName = "articles",
    indices = [Index(value = ["slug"], unique = true), Index(value = ["categoryId"])]
)
data class ArticleEntity(
    @PrimaryKey val id: String,
    val slug: String,
    val title: String,
    val excerpt: String,
    val contentMarkdown: String,
    val coverImageUrl: String?,
    val authorName: String,
    val categoryId: String,
    val categoryName: String,
    val tagsCsv: String,
    val readingTimeMinutes: Int,
    val publishedAt: String,
    val updatedAt: String,
    val isFeatured: Boolean,
    val normalizedSearchText: String,
    // Local user state preserved across syncs
    val isBookmarked: Boolean = false,
    val lastReadPosition: Int = 0,
    val readProgress: Float = 0f,
    val lastReadAt: Long = 0L
)

@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey val id: String,
    val text: String,
    val author: String,
    val source: String?,
    val categoryId: String?,
    val updatedAt: String,
    val isFavorite: Boolean = false
)
