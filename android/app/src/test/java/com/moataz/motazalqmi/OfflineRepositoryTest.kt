package com.moataz.motazalqmi
import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.moataz.motazalqmi.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import okhttp3.mockwebserver.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@RunWith(RobolectricTestRunner::class) @Config(application=Application::class,sdk=[26,29])
class OfflineRepositoryTest {
 private lateinit var db:BlogDatabase
 private lateinit var server:MockWebServer
 private lateinit var repo:BlogRepository
 private val gson=Gson()
 private val article=ArticleDto(id="a",slug="slug-a",title="إبداع",content="النص الأول",author="معتز",category="books")
 private fun snapshot(items:List<ArticleDto>)=Snapshot(1,"snapshot",true,items,listOf(SectionDto("books","من بطون الكتب")),total=items.size)
 @Before fun setup(){val context=ApplicationProvider.getApplicationContext<Context>();db=Room.inMemoryDatabaseBuilder(context,BlogDatabase::class.java).build();server=MockWebServer();server.start();val api=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create(gson)).build().create(BlogApi::class.java);repo=BlogRepository(db,api,gson,context)}
 @After fun close(){db.close();server.shutdown()}
 private fun enqueue(value:Snapshot){server.enqueue(MockResponse().setBody(gson.toJson(value)))}
 @Test fun repeatSyncPreservesSavedPositionAndEdits()=runBlocking{
  enqueue(snapshot(listOf(article)));repo.sync();repo.toggleSaved("a");repo.progress("a",0,321,0.42f)
  repeat(2){enqueue(snapshot(listOf(article.copy(content="النص المعدل"))));repo.sync()}
  val data=repo.library.first();assertEquals(1,data.articles.size);assertEquals("النص المعدل",data.articles.single().content);assertTrue(data.states.getValue("a").saved);assertEquals(321,data.states.getValue("a").offset)
 }
 @Test fun unchangedSnapshotKeepsPositionAndUsesValidator()=runBlocking {
  server.enqueue(MockResponse().setHeader("ETag","\"v1\"").setBody(gson.toJson(snapshot(listOf(article)))))
  repo.sync()
  repo.progress("a",0,180,0.25f)
  server.enqueue(MockResponse().setResponseCode(304))
  repo.sync()
  server.takeRequest()
  assertEquals("\"v1\"",server.takeRequest().getHeader("If-None-Match"))
  assertEquals(180,repo.library.first().states.getValue("a").offset)
 }
 @Test fun deletionAndCleanupPreserveSavedAndHistory()=runBlocking{
  enqueue(snapshot(listOf(article)));repo.sync();repo.progress("a",0,200,0.3f)
  enqueue(snapshot(emptyList()));repo.sync();repo.cleanCache()
  val data=repo.library.first();assertTrue(data.articles.isNotEmpty());assertFalse(data.available.contains("a"));assertEquals(200,data.states.getValue("a").offset)
 }
 @Test fun incompleteDraftAndMalformedResponsesNeverEraseStoredContent()=runBlocking{
  enqueue(snapshot(listOf(article)));repo.sync()
  for(bad in listOf(snapshot(emptyList()).copy(complete=false),snapshot(listOf(article.copy(status="draft"))),snapshot(listOf(article,article)))){
   enqueue(bad);assertTrue(runCatching{repo.sync()}.isFailure);assertEquals("a",repo.library.first().articles.single().id)
  }
  server.enqueue(MockResponse().setResponseCode(503));assertTrue(runCatching{repo.sync()}.isFailure)
 }
 @Test fun offlineFirstLaunchMigratesOldBookmarksIdempotently()=runBlocking{
  val context=ApplicationProvider.getApplicationContext<Context>();java.io.File(context.filesDir,"room-import-complete").delete()
  context.getSharedPreferences("motazalqmi-reader-v1",Context.MODE_PRIVATE).edit().putStringSet("saved",setOf("seed-1")).commit()
  repo.initialize();assertTrue(repo.library.first().articles.isNotEmpty());assertTrue(repo.library.first().states.getValue("seed-1").saved)
  repo.toggleSaved("seed-1");repo.initialize();assertFalse(repo.library.first().states.getValue("seed-1").saved)
 }
}
