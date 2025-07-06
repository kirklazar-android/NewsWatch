package com.newswatch.core

import androidx.paging.PagingData
import com.newswatch.core.model.Article
import com.newswatch.core.model.Bookmark
import com.newswatch.core.model.FeedRequest
import com.newswatch.core.repository.BookmarkRepository
import com.newswatch.core.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryContractTest {
    private val article = Article("a1", "Headline", "Description", null, "Source", null, "https://example.com/a1", null)

    @Test fun `fake news repository exposes feed search and article lookup`() = runTest {
        val fake = FakeNewsRepository(article)
        assertEquals(article, fake.getArticle("a1"))
        assertEquals(null, fake.getArticle("missing"))
        assertEquals(listOf(article), fake.feedArticles.value)
        assertEquals(listOf(article), fake.searchArticles.value)
    }

    @Test fun `fake bookmark repository saves removes searches and observes state`() = runTest {
        val fake = FakeBookmarkRepository()
        assertEquals(false, fake.isBookmarked("a1").first())
        assertTrue(fake.saveBookmark(article).isSuccess)
        assertEquals(true, fake.isBookmarked("a1").first())
        assertEquals(1, fake.observeBookmarks().first().size)
        assertEquals(1, fake.searchBookmarks("headline").first().size)
        assertTrue(fake.removeBookmark("a1").isSuccess)
        assertEquals(false, fake.isBookmarked("a1").first())
    }

    private class FakeNewsRepository(private val article: Article) : NewsRepository {
        val feedArticles = MutableStateFlow(listOf(article))
        val searchArticles = MutableStateFlow(listOf(article))
        override fun observeFeed(request: FeedRequest): Flow<PagingData<Article>> = feedArticles.map { PagingData.from(it) }
        override fun search(query: String, request: FeedRequest): Flow<PagingData<Article>> = searchArticles.map { PagingData.from(it) }
        override suspend fun getArticle(id: String): Article? = article.takeIf { it.id == id }
    }

    private class FakeBookmarkRepository : BookmarkRepository {
        private val state = MutableStateFlow<List<Bookmark>>(emptyList())
        override fun observeBookmarks(): Flow<List<Bookmark>> = state
        override fun searchBookmarks(query: String): Flow<List<Bookmark>> = state.map { bookmarks -> bookmarks.filter { it.article.title.contains(query, ignoreCase = true) } }
        override fun isBookmarked(id: String): Flow<Boolean> = state.map { bookmarks -> bookmarks.any { it.article.id == id } }
        override suspend fun getBookmark(id: String): Bookmark? = state.value.firstOrNull { it.article.id == id }
        override suspend fun saveBookmark(article: Article): Result<Unit> {
            state.value = (state.value.filterNot { it.article.id == article.id } + Bookmark(article, 1L))
            return Result.success(Unit)
        }
        override suspend fun removeBookmark(id: String): Result<Unit> {
            state.value = state.value.filterNot { it.article.id == id }
            return Result.success(Unit)
        }
    }
}
