package com.moataz.motazalqmi.di

import android.content.Context
import androidx.room.Room
import com.moataz.motazalqmi.BuildConfig
import com.moataz.motazalqmi.data.local.*
import com.moataz.motazalqmi.data.remote.MotazApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModules {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MotazDatabase =
        Room.databaseBuilder(context, MotazDatabase::class.java, "motaz_blog.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideCategoryDao(db: MotazDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideArticleDao(db: MotazDatabase): ArticleDao = db.articleDao()
    @Provides fun provideQuoteDao(db: MotazDatabase): QuoteDao = db.quoteDao()

    @Provides
    @Singleton
    fun provideOkHttp(@ApplicationContext context: Context): OkHttpClient {
        val cache = Cache(File(context.cacheDir, "http_cache"), 15L * 1024L * 1024L)
        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): MotazApiService =
        retrofit.create(MotazApiService::class.java)
}
