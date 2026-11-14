package com.moataz.motazalqmi.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(
    val ok: Boolean,
    val version: String?,
    val data: T?,
    val meta: ApiMeta?,
    val error: ApiError?
)

data class ApiMeta(
    val page: Int? = null,
    val limit: Int? = null,
    val total: Int? = null,
    val hasMore: Boolean? = null,
    val serverTime: String? = null
)

data class ApiError(
    val code: String,
    val message: String
)

data class CategoryDto(
    val id: String,
    val slug: String,
    @SerializedName("name_ar") val nameAr: String,
    val description: String? = "",
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("article_count") val articleCount: Int = 0
)

data class ArticleDto(
    val id: String,
    val slug: String,
    val title: String,
    val excerpt: String? = "",
    @SerializedName("content_markdown") val contentMarkdown: String? = "",
    @SerializedName("cover_image_url") val coverImageUrl: String? = null,
    @SerializedName("author_name") val authorName: String? = "معتز العلقمي",
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String? = "",
    val tags: List<String>? = emptyList(),
    @SerializedName("reading_time_minutes") val readingTimeMinutes: Int = 3,
    @SerializedName("published_at") val publishedAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("is_featured") val isFeatured: Boolean = false
)

data class QuoteDto(
    val id: String,
    val text: String,
    val author: String? = "معتز العلقمي",
    val source: String? = null,
    @SerializedName("category_id") val categoryId: String? = "quotes",
    @SerializedName("updated_at") val updatedAt: String
)

data class BootstrapPayload(
    val categories: List<CategoryDto>,
    val featured: ArticleDto?,
    val latestArticles: List<ArticleDto>,
    val quotes: List<QuoteDto>,
    val serverTime: String
)

data class SyncPayload(
    val articles: List<ArticleDto>,
    val deletedArticleIds: List<String>,
    val quotes: List<QuoteDto>,
    val deletedQuoteIds: List<String>,
    val categories: List<CategoryDto>,
    val serverTime: String
)
