package com.moataz.motazalqmi

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.work.*
import com.google.gson.Gson
import com.moataz.motazalqmi.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@HiltAndroidApp class BlogApplication : Application() {
 override fun onCreate() {
  super.onCreate()
  val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()
  val work = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
   .setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("blog-content-v2", ExistingPeriodicWorkPolicy.KEEP, work)
 }
}
@Module @InstallIn(SingletonComponent::class) object Services {
 @Provides @Singleton fun gson() = Gson()
 @Provides @Singleton fun database(@ApplicationContext context: Context) = Room.databaseBuilder(context, BlogDatabase::class.java, "blog-reader-v2.db").build()
 @Provides @Singleton fun api(gson: Gson): BlogApi {
  val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
   .followRedirects(false).followSslRedirects(false).addInterceptor { chain ->
    val response = chain.proceed(chain.request().newBuilder().header("Accept", "application/json").build())
    if ((response.body?.contentLength() ?: 0) > 24 * 1024 * 1024) { response.close(); throw java.io.IOException("Response too large") }
    response
   }.build()
  return Retrofit.Builder().baseUrl("https://motazalqmi.vercel.app/").client(client)
   .addConverterFactory(GsonConverterFactory.create(gson)).build().create(BlogApi::class.java)
 }
}
@dagger.hilt.EntryPoint @InstallIn(SingletonComponent::class) interface WorkerDependencies { fun repository(): BlogRepository }
class SyncWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
 override suspend fun doWork(): Result {
  val repository = dagger.hilt.android.EntryPointAccessors.fromApplication(applicationContext, WorkerDependencies::class.java).repository()
  return try { repository.initialize(); repository.sync(); Result.success() }
  catch (e: CancellationException) { throw e }
  catch (_: Exception) { if (runAttemptCount < 5) Result.retry() else Result.failure() }
 }
}
