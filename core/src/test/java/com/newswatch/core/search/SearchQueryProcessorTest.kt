package com.newswatch.core.search

import androidx.paging.PagingData
import com.newswatch.core.model.Article
import com.newswatch.core.model.FeedRequest
import com.newswatch.core.repository.NewsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchQueryProcessorTest {
    private val article = Article("a", "Android", null, null, "source", null, "https://example.com/a", null)

    @Test fun `short and offline queries do not call repository`() = runTest {
        val repository = RecordingRepository()
        val queries = MutableStateFlow("a")
        val job = launch { SearchQueryProcessor(repository).observe(queries, FeedRequest(), flowOf(false)).collect {} }
        advanceTimeBy(400)
        assertEquals(emptyList<String>(), repository.queries)
        job.cancel()
    }

    @Test fun `query is trimmed normalized debounced and sent once`() = runTest {
        val repository = RecordingRepository()
        val queries = MutableStateFlow("")
        val job = launch { SearchQueryProcessor(repository).observe(queries, FeedRequest(), flowOf(true)).collect {} }
        runCurrent()
        queries.value = "  Android   News  "
        advanceTimeBy(299)
        assertEquals(emptyList<String>(), repository.queries)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf("android news"), repository.queries)
        queries.value = " android news "
        advanceTimeBy(300)
        assertEquals(listOf("android news"), repository.queries)
        job.cancel()
    }

    private class RecordingRepository : NewsRepository {
        val queries = mutableListOf<String>()
        override fun observeFeed(request: FeedRequest): Flow<PagingData<Article>> = emptyFlow()
        override fun search(query: String, request: FeedRequest): Flow<PagingData<Article>> { queries += query; return flowOf(PagingData.from(listOf(Article("a", "Android", null, null, "source", null, "https://example.com/a", null)))) }
        override suspend fun getArticle(id: String): Article? = Article("a", "Android", null, null, "source", null, "https://example.com/a", null).takeIf { it.id == id }
    }
}