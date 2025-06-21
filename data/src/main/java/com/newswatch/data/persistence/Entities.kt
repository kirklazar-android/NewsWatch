package com.newswatch.data.persistence

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "cached_articles", indices = [Index(value = ["feedPosition"], unique = true), Index(value = ["cachedAt"])])
data class CachedArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val content: String?,
    val sourceName: String,
    val imageUrl: String?,
    val originalUrl: String,
    val publishedAt: String?,
    val feedPosition: Int,
    val cachedAt: Long,
)

@Entity(tableName = "bookmarked_articles", indices = [Index(value = ["bookmarkedAt"]), Index(value = ["title"]), Index(value = ["sourceName"])])
data class BookmarkedArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val content: String?,
    val sourceName: String,
    val imageUrl: String?,
    val originalUrl: String,
    val publishedAt: String?,
    val bookmarkedAt: Long,
)

@Entity(tableName = "feed_metadata")
data class FeedMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val requestKey: String,
    val nextPage: Int,
    val endOfPagination: Boolean,
    val lastSuccessfulRefresh: Long?,
) {
    companion object { const val SINGLETON_ID = 0 }
}