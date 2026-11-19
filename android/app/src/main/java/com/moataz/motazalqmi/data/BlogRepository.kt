package com.moataz.motazalqmi.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface BlogApi {
 @GET("api/v1/sync")
 suspend fun snapshot(@retrofit2.http.Header("If-None-Match") etag: String?): retrofit2.Response<Snapshot>
}
data class Library(val articles: List<ArticleDto> = emptyList(), val states: Map<String, ReaderState> = emptyMap(),
 val sections: List<SectionDto> = emptyList(), val quotes: List<QuoteDto> = emptyList(),
 val available: Set<String> = emptySet(), val syncedAt: Long = 0, val about: String = "",
 val siteName: String = "مدونة معتز العلقمي", val tagline: String = "حيث تلتقي الكلمة بالفكر، ويزهر الأدب",
 val authors: List<AuthorProfileDto> = emptyList())

@Singleton class BlogRepository @Inject constructor(
 private val db: BlogDatabase, private val api: BlogApi, private val gson: Gson,
 @ApplicationContext private val context: Context
) {
 private val mutex = Mutex()
 private val syncMutex = Mutex()
 private val dao = db.dao()
 val library = combine(dao.articles(), dao.states(), dao.metadata()) { rows, states, metadata ->
  val m = metadata.associate { it.key to it.payload }
  Library(rows.mapNotNull { runCatching { gson.fromJson(it.payload, ArticleDto::class.java) }.getOrNull() }
   .sortedByDescending { it.publishDate }, states.associateBy { it.id },
   parseList<SectionDto>(m["sections"]),
   (parseList<QuoteDto>(m["quotes"]) + metadata.filter { m -> m.key.startsWith("saved_quote:") && states.any { it.id == m.key.removePrefix("saved_quote:") && it.saved } }
    .mapNotNull { runCatching { gson.fromJson(it.payload, QuoteDto::class.java) }.getOrNull() }).distinctBy { quoteId(it) },
   rows.filter { it.available }.map { it.id }.toSet(), m["synced_at"]?.toLongOrNull() ?: 0,
   m["about"] ?: "", m["site_name"] ?: "مدونة معتز العلقمي",
   m["tagline"] ?: "حيث تلتقي الكلمة بالفكر، ويزهر الأدب", parseList<AuthorProfileDto>(m["authors"]))
 }
 private inline fun <reified T> parseList(raw: String?): List<T> = runCatching {
  // Construct the type from concrete classes so release shrinking cannot erase its generic signature.
  val listType = TypeToken.getParameterized(List::class.java, T::class.java).type
  gson.fromJson<List<T>>(raw ?: "[]", listType) ?: emptyList()
 }.getOrDefault(emptyList())
 suspend fun initialize() = withContext(Dispatchers.IO) { mutex.withLock {
  val marker = File(context.filesDir, "room-import-complete")
  if (!marker.exists()) {
   val legacy = File(context.filesDir, "published-content-v1.json")
   val seed = runCatching {
    gson.fromJson(if (legacy.exists()) legacy.readText() else context.assets.open("bootstrap.json").bufferedReader().use { it.readText() }, Snapshot::class.java)
   }.getOrElse { gson.fromJson(context.assets.open("bootstrap.json").bufferedReader().use { it.readText() }, Snapshot::class.java) }
   if (dao.count() == 0) applySnapshot(seed.copy(complete = true), false)
   val old = context.getSharedPreferences("motazalqmi-reader-v1", Context.MODE_PRIVATE)
   old.getStringSet("saved", emptySet())?.forEach { dao.putState((dao.state(it) ?: ReaderState(it)).copy(saved = true)) }
   marker.writeText("1")
  }
 } }
 suspend fun sync() = withContext(Dispatchers.IO) { syncMutex.withLock {
  val previous = dao.metadataValue("sync_etag")?.takeIf { it.isNotBlank() }
  val response = api.snapshot(previous)
  if (response.code() == 304 && previous != null) {
   dao.putMetadata(listOf(Metadata("synced_at", System.currentTimeMillis().toString())))
   return@withLock
  }
  if (!response.isSuccessful) throw retrofit2.HttpException(response)
  val snapshot = requireNotNull(response.body()) { "Missing snapshot" }
  require(snapshot.apiVersion == 1 && snapshot.complete && snapshot.mode == "snapshot") { "Incomplete snapshot" }
  mutex.withLock { applySnapshot(snapshot, true, response.headers()["ETag"].orEmpty()) }
 } }
 private suspend fun applySnapshot(s: Snapshot, network: Boolean, etag: String = "") {
  require(s.articles.size <= 2000 && s.total == s.articles.size)
  require(s.articles.all { it.id.isNotBlank() && it.slug.isNotBlank() && it.status == "published" })
  require(s.articles.map { it.id }.toSet().size == s.articles.size)
  require(s.sections.isNotEmpty())
  db.withTransaction {
   dao.retireAll()
   dao.putArticles(s.articles.map { ArticleEntity(it.id, gson.toJson(it)) })
   dao.putMetadata(listOf(Metadata("sections", gson.toJson(s.sections)), Metadata("quotes", gson.toJson(s.quotes)),
    Metadata("authors", gson.toJson(s.authors)),
    Metadata("about", s.site["about"]?.toString() ?: ""),
    Metadata("site_name", s.site["name"]?.toString() ?: "مدونة معتز العلقمي"),
    Metadata("tagline", s.site["tagline"]?.toString() ?: "")) +
    if (network) listOf(Metadata("synced_at", System.currentTimeMillis().toString()), Metadata("sync_etag", etag)) else emptyList())
   dao.prune()
  }
 }

 /** Prefetch published cover and Markdown images to a persistent private Coil disk cache.
  * Background work observes the user's Wi-Fi preference; failures never erase articles. */
 suspend fun prefetchImages(wifiOnly:Boolean) = withContext(Dispatchers.IO) {
  val cm=context.getSystemService(ConnectivityManager::class.java)
  val wifi=cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true
  if(wifiOnly&&!wifi)return@withContext
  val current=dao.articles().first().filter { it.available }.mapNotNull {
   runCatching { gson.fromJson(it.payload,ArticleDto::class.java) }.getOrNull()
  }
  val rx=Regex("""!\[[^\]]{0,300}]\(\s*<?(https://[^)\s>]+)>?(?:\s+["'][^"']*["'])?\s*\)""")
  val urls=current.flatMap { a -> listOf(a.coverImage,a.authorImage)+rx.findAll(a.content).map { it.groupValues[1] }.toList() }
   .filter {
    it.startsWith("https://")&&it.length<=2048&&
     runCatching { java.net.URI(it).path }.getOrNull().orEmpty().substringAfterLast('.').lowercase() !in
      setOf("mp4","webm","mp3","m4a","ogg","pdf")
   }.distinct().take(120)
  val loader=context.imageLoader
  for(url in urls){
   runCatching { loader.execute(ImageRequest.Builder(context).data(readerImageUrl(url)).size(720,480)
    .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.ENABLED).build()) }
  }
 }

 suspend fun toggleSaved(id: String) = mutex.withLock {
  val state = dao.state(id) ?: ReaderState(id)
  if (!state.saved && id.startsWith("quote:")) {
   val current = dao.metadata().first().find { it.key == "quotes" }?.payload
   parseList<QuoteDto>(current).find { quoteId(it) == id }?.let {
    dao.putMetadata(listOf(Metadata("saved_quote:$id", gson.toJson(it))))
   }
  }
  dao.putState(state.copy(saved = !state.saved))
 }
 suspend fun progress(id: String, block: Int, offset: Int, value: Float) = mutex.withLock {
  val state = dao.state(id) ?: ReaderState(id)
  dao.putState(state.copy(block = block, offset = offset, progress = value.coerceIn(0f, 1f), openedAt = System.currentTimeMillis()))
 }
 suspend fun cleanCache() = withContext(Dispatchers.IO) { syncMutex.withLock { mutex.withLock {
  db.withTransaction {
   dao.clearUnsaved()
   dao.putMetadata(listOf(Metadata("sync_etag", "")))
  }
  context.imageLoader.diskCache?.clear()
 } } }
}
