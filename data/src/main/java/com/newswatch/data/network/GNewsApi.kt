package com.newswatch.data.network

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class GNewsResponse(val totalArticles: Int = 0, val articles: List<GNewsArticle> = emptyList())

@JsonClass(generateAdapter = true)
data class GNewsArticle(
    val id: String? = null,
    val title: String = "",
    val description: String? = null,
    val content: String? = null,
    val image: String? = null,
    val publishedAt: String? = null,
    val url: String = "",
    val source: GNewsSource = GNewsSource(),
)

@JsonClass(generateAdapter = true)
data class GNewsSource(val name: String = "")

interface GNewsApi {
    @GET("top-headlines")
    suspend fun topHeadlines(
        @Query("country") country: String,
        @Query("lang") language: String,
        @Query("category") category: String? = null,
        @Query("page") page: Int,
        @Query("max") max: Int,
    ): GNewsResponse

    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("lang") language: String,
        @Query("country") country: String,
        @Query("page") page: Int,
        @Query("max") max: Int,
    ): GNewsResponse
}