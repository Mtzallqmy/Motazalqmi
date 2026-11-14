package com.moataz.motazalqmi.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE isFeatured = 1 ORDER BY publishedAt DESC LIMIT 1")
    fun observeFeatured(): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE categoryId = :categoryId ORDER BY publishedAt DESC")
    fun observeByCategory(categoryId: String): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE slug = :slug LIMIT 1")
    fun observeBySlug(slug: String): Flow<ArticleEntity?>

    @Query("SELECT * FROM articles WHERE slug = :slug LIMIT 1")
    suspend fun getBySlug(slug: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE isBookmarked = 1 ORDER BY publishedAt DESC")
    fun observeBookmarked(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE lastReadAt > 0 ORDER BY lastReadAt DESC LIMIT 30")
    fun observeHistory(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE readProgress > 0.05 AND readProgress < 0.95 ORDER BY lastReadAt DESC")
    fun observeUnfinished(): Flow<List<ArticleEntity>>

    @Query("SELECT * FROM articles WHERE normalizedSearchText LIKE '%' || :normalizedQuery || '%' ORDER BY publishedAt DESC")
    fun searchNormalized(normalizedQuery: String): Flow<List<ArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE articles SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun setBookmarked(id: String, bookmarked: Boolean)

    @Query("UPDATE articles SET lastReadPosition = :position, readProgress = :progress, lastReadAt = :timestamp WHERE id = :id")
    suspend fun updateReadingProgress(id: String, position: Int, progress: Float, timestamp: Long)

    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND lastReadAt = 0")
    suspend fun clearNonSavedCache()
}

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quotes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY updatedAt DESC")
    fun observeFavorites(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(quotes: List<QuoteEntity>)

    @Query("DELETE FROM quotes WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("UPDATE quotes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)
}
