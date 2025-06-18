package com.newswatch.core.repository

import androidx.paging.PagingData
import com.newswatch.core.model.Article
import com.newswatch.core.model.Bookmark
import com.newswatch.core.model.FeedRequest
import kotlinx.coroutines.flow.Flow

interface NewsRepository {
    fun observeFeed(request: FeedRequest): Flow<PagingData<Article>>
    fun search(query: String, request: FeedRequest): Flow<PagingData<Article>>
    suspend fun getArticle(id: String): Article?
}

interface BookmarkRepository {
    fun observeBookmarks(): Flow<List<Bookmark>>
    fun searchBookmarks(query: String): Flow<List<Bookmark>>
    fun isBookmarked(id: String): Flow<Boolean>
    suspend fun getBookmark(id: String): Bookmark?
    suspend fun saveBookmark(article: Article): Result<Unit>
    suspend fun removeBookmark(id: String): Result<Unit>
}
