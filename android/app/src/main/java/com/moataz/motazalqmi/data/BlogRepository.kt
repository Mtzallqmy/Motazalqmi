package com.moataz.motazalqmi.data

import android.content.Context
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

interface BlogApi { @GET("api/v1/sync") suspend fun snapshot(): Snapshot }
data class Library(val articles: List<ArticleDto> = emptyList(), val states: Map<String, ReaderState> = emptyMap(),
 val sections: List<SectionDto> = emptyList(), val quotes: List<QuoteDto> = emptyList(),
 val available: Set<String> = emptySet(), val syncedAt: Long = 0, val about: String = "")

@Singleton class BlogRepository @Inject constructor(
 private val db: BlogDatabase, private val api: BlogApi, private val gson: Gson,
 @ApplicationContext private val context: Context
) {
 private val mutex = Mutex()
 private val dao = db.dao()
 val library = combine(dao.articles(), dao.states(), dao.metadata()) { rows, states, metadata ->
  val m = metadata.associate { it.key to it.payload }
  Library(rows.mapNotNull { runCatching { gson.fromJson(it.payload, ArticleDto::class.java) }.getOrNull() }
   .sortedByDescending { it.publishDate }, states.associateBy { it.id },
   parseList<SectionDto>(m["sections"]), parseList<QuoteDto>(m["quotes"]),
   rows.filter { it.available }.map { it.id }.toSet(), m["synced_at"]?.toLongOrNull() ?: 0,
   m["about"] ?: "")
 }
 private inline fun <reified T> parseList(raw: String?): List<T> = runCatching {
  gson.fromJson<List<T>>(raw ?: "[]", object : TypeToken<List<T>>() {}.type)
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
 suspend fun sync() = withContext(Dispatchers.IO) { mutex.withLock {
  val snapshot = api.snapshot()
  require(snapshot.apiVersion == 1 && snapshot.complete && snapshot.mode == "snapshot") { "Incomplete snapshot" }
  applySnapshot(snapshot, true)
 } }
 private suspend fun applySnapshot(s: Snapshot, network: Boolean) {
  require(s.articles.size <= 2000 && s.total == s.articles.size)
  require(s.articles.all { it.id.isNotBlank() && it.slug.isNotBlank() && it.status == "published" })
  require(s.articles.map { it.id }.toSet().size == s.articles.size)
  require(s.sections.isNotEmpty())
  db.withTransaction {
   dao.retireAll()
   dao.putArticles(s.articles.map { ArticleEntity(it.id, gson.toJson(it)) })
   dao.putMetadata(listOf(Metadata("sections", gson.toJson(s.sections)), Metadata("quotes", gson.toJson(s.quotes)),
    Metadata("about", s.site["about"]?.toString() ?: "")) +
    if (network) listOf(Metadata("synced_at", System.currentTimeMillis().toString())) else emptyList())
   dao.prune()
  }
 }
 suspend fun toggleSaved(id: String) = mutex.withLock {
  val state = dao.state(id) ?: ReaderState(id)
  dao.putState(state.copy(saved = !state.saved))
 }
 suspend fun progress(id: String, block: Int, offset: Int, value: Float) = mutex.withLock {
  val state = dao.state(id) ?: ReaderState(id)
  dao.putState(state.copy(block = block, offset = offset, progress = value.coerceIn(0f, 1f), openedAt = System.currentTimeMillis()))
 }
 suspend fun cleanCache() = withContext(Dispatchers.IO) { mutex.withLock {
  dao.clearUnsaved(); File(context.cacheDir, "image_cache").deleteRecursively()
 } }
}
