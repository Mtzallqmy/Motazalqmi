package com.moataz.motazalqmi.data.repository

import com.moataz.motazalqmi.data.local.*
import com.moataz.motazalqmi.data.preferences.ReaderPreferencesRepository
import com.moataz.motazalqmi.data.remote.MotazApiService
import com.moataz.motazalqmi.data.remote.dto.ArticleDto
import com.moataz.motazalqmi.data.remote.dto.CategoryDto
import com.moataz.motazalqmi.data.remote.dto.QuoteDto
import com.moataz.motazalqmi.domain.model.Article
import com.moataz.motazalqmi.domain.model.Category
import com.moataz.motazalqmi.domain.model.Quote
import com.moataz.motazalqmi.domain.model.SyncResult
import com.moataz.motazalqmi.domain.text.ArabicTextNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlogRepository @Inject constructor(
    private val api: MotazApiService,
    private val categoryDao: CategoryDao,
    private val articleDao: ArticleDao,
    private val quoteDao: QuoteDao,
    private val prefs: ReaderPreferencesRepository
) {
    suspend fun ensureSeedCategories() {
        if (categoryDao.count() == 0) {
            categoryDao.upsertAll(SeedData.officialCategories)
        }
    }

    fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeAllArticles(): Flow<List<Article>> =
        articleDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeFeaturedArticle(): Flow<Article?> =
        articleDao.observeFeatured().map { it?.toDomain() }

    fun observeArticlesByCategory(categoryId: String): Flow<List<Article>> =
        articleDao.observeByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    fun observeArticleBySlug(slug: String): Flow<Article?> =
        articleDao.observeBySlug(slug).map { it?.toDomain() }

    fun observeBookmarkedArticles(): Flow<List<Article>> =
        articleDao.observeBookmarked().map { list -> list.map { it.toDomain() } }

    fun observeHistory(): Flow<List<Article>> =
        articleDao.observeHistory().map { list -> list.map { it.toDomain() } }

    fun observeUnfinished(): Flow<List<Article>> =
        articleDao.observeUnfinished().map { list -> list.map { it.toDomain() } }

    fun searchArticles(query: String): Flow<List<Article>> {
        val normalized = ArabicTextNormalizer.normalizeForSearch(query)
        return articleDao.searchNormalized(normalized).map { list -> list.map { it.toDomain() } }
    }

    fun observeQuotes(): Flow<List<Quote>> =
        quoteDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeFavoriteQuotes(): Flow<List<Quote>> =
        quoteDao.observeFavorites().map { list -> list.map { it.toDomain() } }

    suspend fun toggleBookmark(articleId: String, bookmarked: Boolean) {
        articleDao.setBookmarked(articleId, bookmarked)
    }

    suspend fun toggleFavoriteQuote(quoteId: String, favorite: Boolean) {
        quoteDao.setFavorite(quoteId, favorite)
    }

    suspend fun saveReadingProgress(articleId: String, position: Int, progress: Float) {
        articleDao.updateReadingProgress(articleId, position, progress.coerceIn(0f, 1f), System.currentTimeMillis())
    }

    suspend fun clearTemporaryCachePreservingSaved() {
        articleDao.clearNonSavedCache()
    }

    suspend fun syncIncremental(): SyncResult {
        return try {
            ensureSeedCategories()
            val since = prefs.settingsFlow.first().lastSyncIso
            val response = api.syncSince(since)
            val payload = response.data ?: return SyncResult.Error(response.error?.message ?: "استجابة مزامنة فارغة")

            if (payload.categories.isNotEmpty()) {
                categoryDao.upsertAll(payload.categories.map { it.toEntity() })
            }

            if (payload.deletedArticleIds.isNotEmpty()) {
                articleDao.deleteByIds(payload.deletedArticleIds)
            }
            if (payload.deletedQuoteIds.isNotEmpty()) {
                quoteDao.deleteByIds(payload.deletedQuoteIds)
            }

            val mergedArticles = payload.articles.map { dto ->
                val existing = articleDao.getById(dto.id)
                dto.toEntity(existing)
            }
            if (mergedArticles.isNotEmpty()) {
                articleDao.upsertAll(mergedArticles)
            }

            val mergedQuotes = payload.quotes.map { dto ->
                val existing = quoteDao.getById(dto.id)
                dto.toEntity(existing)
            }
            if (mergedQuotes.isNotEmpty()) {
                quoteDao.upsertAll(mergedQuotes)
            }

            prefs.setLastSyncIso(payload.serverTime)
            SyncResult.Success(
                syncedAtIso = payload.serverTime,
                updatedCount = mergedArticles.size + mergedQuotes.size,
                deletedCount = payload.deletedArticleIds.size + payload.deletedQuoteIds.size
            )
        } catch (e: Exception) {
            SyncResult.Error(e.localizedMessage ?: "تعذّر الاتصال بالخادم")
        }
    }

    private fun CategoryEntity.toDomain() = Category(id, slug, nameAr, description, sortOrder, articleCount)

    private fun ArticleEntity.toDomain() = Article(
        id, slug, title, excerpt, contentMarkdown, coverImageUrl, authorName,
        categoryId, categoryName, tagsCsv.split(",").filter { it.isNotBlank() },
        readingTimeMinutes, publishedAt, updatedAt, isFeatured, isBookmarked,
        lastReadPosition, readProgress, lastReadAt
    )

    private fun QuoteEntity.toDomain() = Quote(id, text, author, source, categoryId, updatedAt, isFavorite)

    private fun CategoryDto.toEntity() = CategoryEntity(id, slug, nameAr, description.orEmpty(), sortOrder, articleCount)

    private fun ArticleDto.toEntity(existing: ArticleEntity?): ArticleEntity {
        val tagsList = tags.orEmpty()
        val combinedSearch = ArabicTextNormalizer.normalizeForSearch(
            listOf(title, excerpt.orEmpty(), contentMarkdown.orEmpty(), authorName.orEmpty(), categoryName.orEmpty(), tagsList.joinToString(" ")).joinToString(" ")
        )
        return ArticleEntity(
            id = id,
            slug = slug,
            title = title,
            excerpt = excerpt.orEmpty(),
            contentMarkdown = contentMarkdown.orEmpty(),
            coverImageUrl = coverImageUrl,
            authorName = authorName ?: "معتز العلقمي",
            categoryId = categoryId,
            categoryName = categoryName.orEmpty(),
            tagsCsv = tagsList.joinToString(","),
            readingTimeMinutes = readingTimeMinutes,
            publishedAt = publishedAt,
            updatedAt = updatedAt,
            isFeatured = isFeatured,
            normalizedSearchText = combinedSearch,
            isBookmarked = existing?.isBookmarked ?: false,
            lastReadPosition = existing?.lastReadPosition ?: 0,
            readProgress = existing?.readProgress ?: 0f,
            lastReadAt = existing?.lastReadAt ?: 0L
        )
    }

    private fun QuoteDto.toEntity(existing: QuoteEntity?) = QuoteEntity(
        id = id,
        text = text,
        author = author ?: "معتز العلقمي",
        source = source,
        categoryId = categoryId,
        updatedAt = updatedAt,
        isFavorite = existing?.isFavorite ?: false
    )
}
