package com.newswatch.data.persistence

import com.newswatch.core.model.Article
import com.newswatch.core.model.Bookmark
import com.newswatch.core.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomBookmarkRepository(private val dao: BookmarkDao) : BookmarkRepository {
    override fun observeBookmarks(): Flow<List<Bookmark>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun searchBookmarks(query: String): Flow<List<Bookmark>> = dao.search(query.trim()).map { rows -> rows.map { it.toDomain() } }

    override fun isBookmarked(id: String): Flow<Boolean> = dao.isBookmarked(id)

    override suspend fun getBookmark(id: String): Bookmark? = dao.getById(id)?.toDomain()

    override suspend fun saveBookmark(article: Article): Result<Unit> = runCatching {
        dao.upsert(article.toBookmarkedEntity(System.currentTimeMillis()))
    }

    override suspend fun removeBookmark(id: String): Result<Unit> = runCatching { dao.delete(id) }
}