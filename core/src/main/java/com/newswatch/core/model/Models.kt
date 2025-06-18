package com.newswatch.core.model

data class Article(
    val id: String,
    val title: String,
    val description: String?,
    val content: String?,
    val sourceName: String,
    val imageUrl: String?,
    val originalUrl: String,
    val publishedAt: String?,
    val feedPosition: Int? = null,
)

data class FeedRequest(
    val country: String = DEFAULT_COUNTRY,
    val language: String = DEFAULT_LANGUAGE,
    val category: String? = null,
) {
    val key: String
        get() = listOf(country, language, category ?: NO_CATEGORY).joinToString(":")

    init {
        require(country == country.trim().lowercase()) { "country must be normalized" }
        require(country.isNotBlank()) { "country must not be blank" }
        require(language == language.trim().lowercase()) { "language must be normalized" }
        require(language.isNotBlank()) { "language must not be blank" }
        require(category == null || category == category.trim().lowercase()) { "category must be normalized" }
    }

    companion object {
        const val DEFAULT_COUNTRY = "in"
        const val DEFAULT_LANGUAGE = "en"
        private const val NO_CATEGORY = "all"

        fun normalized(country: String = DEFAULT_COUNTRY, language: String = DEFAULT_LANGUAGE, category: String? = null): FeedRequest =
            FeedRequest(country.trim().lowercase(), language.trim().lowercase(), category?.trim()?.lowercase()?.ifBlank { null })
    }
}

data class Bookmark(val article: Article, val bookmarkedAt: Long)

sealed interface NewsError {
    val retryable: Boolean

    data object Network : NewsError { override val retryable = true }
    data object Authentication : NewsError { override val retryable = false }
    data object Quota : NewsError { override val retryable = false }
    data class Http(val code: Int) : NewsError { override val retryable = code >= 500 }
    data object Parse : NewsError { override val retryable = false }
    data object Validation : NewsError { override val retryable = false }
    data object Unknown : NewsError { override val retryable = false }
}
