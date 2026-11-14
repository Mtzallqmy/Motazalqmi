package com.moataz.motazalqmi
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import com.google.gson.Gson
import com.moataz.motazalqmi.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class) class SnapshotTest {
 private val context: Context = ApplicationProvider.getApplicationContext()
 private val article = ArticleDto(id="a",slug="real",title="عنوان",content="نص")
 private val section = SectionDto("general","كتابات عامة")
 private fun snapshot(articles: List<ArticleDto>) = Snapshot(1,"snapshot",true,articles,listOf(section),total=articles.size)
 @Test fun snapshotUpdatesAreIdempotentAndPreserveSavedCopiesAndPosition() = runBlocking {
  val db = Room.inMemoryDatabaseBuilder(context,BlogDatabase::class.java).build()
  var remote = snapshot(listOf(article))
  val api = object: BlogApi { override suspend fun snapshot() = remote }
  val repository = BlogRepository(db,api,Gson(),context)
  try {
   repository.sync(); repository.toggleSaved("a"); repository.progress("a",0,240,.4f)
   remote = snapshot(listOf(article.copy(title="عنوان محدث")))
   repository.sync(); repository.sync()
   assertEquals(1,db.dao().count())
   assertEquals("عنوان محدث",repository.library.first().articles.single().title)
   remote = snapshot(emptyList()); repository.sync()
   val after = repository.library.first()
   assertFalse(after.available.contains("a"))
   assertEquals("عنوان محدث",after.articles.single().title)
   assertTrue(after.states.getValue("a").saved)
   assertEquals(240,after.states.getValue("a").offset)
   repository.cleanCache(); assertEquals(1,db.dao().count())
  } finally { db.close() }
 }
 @Test fun incompleteAndDraftResponsesCannotDestroyLocalData() = runBlocking {
  val db = Room.inMemoryDatabaseBuilder(context,BlogDatabase::class.java).build()
  var remote = snapshot(listOf(article))
  val repository = BlogRepository(db,object:BlogApi { override suspend fun snapshot() = remote },Gson(),context)
  try {
   repository.sync()
   remote = snapshot(emptyList()).copy(complete=false)
   assertTrue(runCatching { repository.sync() }.isFailure)
   assertEquals(1,db.dao().count())
   remote = snapshot(listOf(article.copy(status="draft")))
   assertTrue(runCatching { repository.sync() }.isFailure)
   assertEquals("عنوان",repository.library.first().articles.single().title)
   remote = snapshot(listOf(article.copy(title="")))
   assertTrue(runCatching { repository.sync() }.isFailure)
   assertEquals("عنوان",repository.library.first().articles.single().title)
  } finally { db.close() }
 }
}
