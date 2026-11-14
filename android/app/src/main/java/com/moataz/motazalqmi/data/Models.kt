package com.moataz.motazalqmi.data

import androidx.room.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow

data class ArticleDto(
 val id: String = "", val slug: String = "", val title: String = "", val content: String = "",
 val excerpt: String = "", val category: String = "general", val author: String = "",
 val publishDate: String = "", val coverImage: String = "", val featured: Boolean = false,
 val tags: List<String> = emptyList(), val status: String = "published",
 val readTime: String = "5 دقائق", val views: Int = 0, val likes: Int = 0,
 val updatedAt: String = ""
)
data class SectionDto(val id: String = "", val title: String = "", val subtitle: String = "")
data class QuoteDto(val id: String = "", val text: String = "", val author: String = "", val source: String = "")
data class Snapshot(
 val apiVersion: Int = 0, val mode: String = "snapshot", val complete: Boolean = false,
 val articles: List<ArticleDto> = emptyList(), val sections: List<SectionDto> = emptyList(),
 val quotes: List<QuoteDto> = emptyList(), val site: Map<String, Any> = emptyMap(), val total: Int = 0
)
@Entity(tableName = "articles")
data class ArticleEntity(@PrimaryKey val id: String, val payload: String, val available: Boolean = true)
@Entity(tableName = "reader_state")
data class ReaderState(@PrimaryKey val id: String, val saved: Boolean = false,
 val block: Int = 0, val offset: Int = 0, val progress: Float = 0f, val openedAt: Long = 0)
@Entity(tableName = "metadata")
data class Metadata(@PrimaryKey val key: String, val payload: String)
@Dao
interface BlogDao {
 @Query("SELECT * FROM articles") fun articles(): Flow<List<ArticleEntity>>
 @Query("SELECT * FROM reader_state") fun states(): Flow<List<ReaderState>>
 @Query("SELECT * FROM metadata") fun metadata(): Flow<List<Metadata>>
 @Query("SELECT * FROM reader_state WHERE id=:id") suspend fun state(id: String): ReaderState?
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putState(state: ReaderState)
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putArticles(rows: List<ArticleEntity>)
 @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMetadata(rows: List<Metadata>)
 @Query("UPDATE articles SET available=0") suspend fun retireAll()
 @Query("DELETE FROM articles WHERE available=0 AND id NOT IN (SELECT id FROM reader_state WHERE saved=1 OR openedAt>0)") suspend fun prune()
 @Query("DELETE FROM articles WHERE id NOT IN (SELECT id FROM reader_state WHERE saved=1 OR openedAt>0)") suspend fun clearUnsaved()
 @Query("SELECT COUNT(*) FROM articles") suspend fun count(): Int
}
@Database(entities = [ArticleEntity::class, ReaderState::class, Metadata::class], version = 1, exportSchema = true)
abstract class BlogDatabase : RoomDatabase() { abstract fun dao(): BlogDao }

fun quoteId(q: QuoteDto): String = "quote:" + java.security.MessageDigest.getInstance("SHA-256")
 .digest(q.text.toByteArray()).joinToString("") { "%02x".format(it) }


/** First-party origin helps users reach the seven existing images when a third-party CDN is blocked.
 * Arbitrary external images stay direct HTTPS links, not proxyable URLs. */
fun readerImageUrl(url:String):String =
 if(url.startsWith("https://images.unsplash.com/"))
  "https://motazalqmi.vercel.app/api/v1/image?src="+java.net.URLEncoder.encode(url,"UTF-8")
 else url
