package com.moataz.motazalqmi.data.remote

import com.moataz.motazalqmi.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MotazApiService {
    @GET("health")
    suspend fun getHealth(): ApiEnvelope<Map<String, Any>>

    @GET("bootstrap")
    suspend fun getBootstrap(): ApiEnvelope<BootstrapPayload>

    @GET("articles")
    suspend fun getArticles(
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
        @Query("sort") sort: String = "latest"
    ): ApiEnvelope<List<ArticleDto>>

    @GET("articles/{slug}")
    suspend fun getArticleBySlug(@Path("slug") slug: String): ApiEnvelope<ArticleDto>

    @GET("categories")
    suspend fun getCategories(): ApiEnvelope<List<CategoryDto>>

    @GET("quotes")
    suspend fun getQuotes(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): ApiEnvelope<List<QuoteDto>>

    @GET("search")
    suspend fun search(@Query("q") query: String): ApiEnvelope<List<ArticleDto>>

    @GET("featured")
    suspend fun getFeatured(): ApiEnvelope<ArticleDto>

    @GET("sync")
    suspend fun syncSince(@Query("since") sinceIso: String?): ApiEnvelope<SyncPayload>

    @GET("config")
    suspend fun getConfig(): ApiEnvelope<Map<String, Any>>

    @GET("versions")
    suspend fun getVersions(): ApiEnvelope<Map<String, Any>>
}
