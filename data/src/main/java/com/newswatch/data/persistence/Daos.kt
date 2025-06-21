package com.newswatch.data.persistence

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedArticleDao {
    @Query("SELECT * FROM cached_articles ORDER BY feedPosition ASC")
    fun pagingSource(): PagingSource<Int, CachedArticleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(articles: List<CachedArticleEntity>)

    @Query("DELETE FROM cached_articles")
    suspend fun clear()

    @Query("DELETE FROM cached_articles WHERE feedPosition >= :firstPositionToRemove")
    suspend fun deleteFromPosition(firstPositionToRemove: Int)

    @Query("SELECT COUNT(*) FROM cached_articles")
    suspend fun count(): Int

    @Query("SELECT * FROM cached_articles ORDER BY feedPosition ASC")
    suspend fun all(): List<CachedArticleEntity>

    @Query("SELECT * FROM cached_articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedArticleEntity?
}

@Dao
interface FeedMetadataDao {
    @Query("SELECT * FROM feed_metadata WHERE id = :id LIMIT 1")
    suspend fun get(id: Int = FeedMetadataEntity.SINGLETON_ID): FeedMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: FeedMetadataEntity)

    @Query("DELETE FROM feed_metadata")
    suspend fun clear()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarked_articles ORDER BY bookmarkedAt DESC")
    fun observeAll(): Flow<List<BookmarkedArticleEntity>>

    @Query("SELECT * FROM bookmarked_articles WHERE title LIKE '%' || :query || '%' OR sourceName LIKE '%' || :query || '%' ORDER BY bookmarkedAt DESC")
    fun search(query: String): Flow<List<BookmarkedArticleEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarked_articles WHERE id = :id)")
    fun isBookmarked(id: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(article: BookmarkedArticleEntity)

    @Query("DELETE FROM bookmarked_articles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM bookmarked_articles WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookmarkedArticleEntity?

    @Query("SELECT COUNT(*) FROM bookmarked_articles")
    suspend fun count(): Int
}