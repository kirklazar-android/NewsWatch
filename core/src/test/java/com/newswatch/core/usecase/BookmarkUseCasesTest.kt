package com.newswatch.core.usecase

import com.newswatch.core.model.Article
import com.newswatch.core.model.Bookmark
import com.newswatch.core.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookmarkUseCasesTest {
    private val article = Article("a1", "Headline", null, null, "Source", null, "https://example.com/a1", null)

    @Test fun `use cases share repository state across save observe and remove`() = runTest {
        val repository = FakeBookmarkRepository()
        val save = SaveBookmark(repository)
        val observe = ObserveBookmarks(repository)
        val state = ObserveBookmarkState(repository)
        val remove = RemoveBookmark(repository)

        assertTrue(save(article).isSuccess)
        assertEquals(1, observe().first().size)
        assertEquals(true, state("a1").first())
        assertTrue(remove("a1").isSuccess)
        assertEquals(false, state("a1").first())
    }

    @Test fun `search use case trims query`() = runTest {
        val repository = FakeBookmarkRepository()
        SaveBookmark(repository)(article)
        assertEquals(1, SearchBookmarks(repository)(" Head ").first().size)
    }

    private class FakeBookmarkRepository : BookmarkRepository {
        private val state = MutableStateFlow<List<Bookmark>>(emptyList())
        override fun observeBookmarks(): Flow<List<Bookmark>> = state
        override fun searchBookmarks(query: String): Flow<List<Bookmark>> = state.map { rows -> rows.filter { it.article.title.contains(query, true) } }
        override fun isBookmarked(id: String): Flow<Boolean> = state.map { rows -> rows.any { it.article.id == id } }
        override suspend fun getBookmark(id: String): Bookmark? = state.value.firstOrNull { it.article.id == id }
        override suspend fun saveBookmark(article: Article): Result<Unit> { state.value = listOf(Bookmark(article, 1L)); return Result.success(Unit) }
        override suspend fun removeBookmark(id: String): Result<Unit> { state.value = state.value.filterNot { it.article.id == id }; return Result.success(Unit) }
    }
}