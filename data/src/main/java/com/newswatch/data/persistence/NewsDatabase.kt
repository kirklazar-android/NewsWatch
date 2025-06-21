package com.newswatch.data.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Database(
    entities = [CachedArticleEntity::class, BookmarkedArticleEntity::class, FeedMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NewsDatabase : RoomDatabase() {
    abstract fun cachedArticleDao(): CachedArticleDao
    abstract fun feedMetadataDao(): FeedMetadataDao
    abstract fun bookmarkDao(): BookmarkDao

    @Transaction
    open suspend fun replaceFeed(
        articles: List<CachedArticleEntity>,
        metadata: FeedMetadataEntity,
    ) {
        cachedArticleDao().clear()
        cachedArticleDao().insertAll(articles)
        feedMetadataDao().upsert(metadata)
    }

    @Transaction
    open suspend fun appendFeed(
        articles: List<CachedArticleEntity>,
        metadata: FeedMetadataEntity,
    ) {
        cachedArticleDao().insertAll(articles)
        feedMetadataDao().upsert(metadata)
    }

    @Transaction
    open suspend fun trimFeed(limit: Int = DEFAULT_FEED_LIMIT) {
        require(limit > 0) { "Feed trim limit must be positive" }
        cachedArticleDao().deleteFromPosition(limit)
    }

    companion object { const val DEFAULT_FEED_LIMIT = 150 }
}