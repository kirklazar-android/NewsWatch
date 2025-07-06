package com.newswatch.data.search

import androidx.paging.PagingSource
import com.newswatch.core.model.FeedRequest
import com.newswatch.data.network.GNewsApi
import com.newswatch.data.network.GNewsArticle
import com.newswatch.data.network.GNewsResponse
import com.newswatch.data.network.GNewsSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class NewsSearchPagingSourceTest {
    private val request = FeedRequest()

    @Test fun `loads search results and next page`() = runTest {
        val source = NewsSearchPagingSource(FakeApi { GNewsResponse(20, (1..10).map { article(if (it == 1) "one" else if (it == 2) "two" else "article-$it") }) }, "android", request)
        val result = source.load(PagingSource.LoadParams.Refresh(null, 10, false)) as PagingSource.LoadResult.Page
        assertEquals(listOf("one", "two"), result.data.take(2).map { it.title })
        assertEquals(2, result.nextKey)
        assertEquals("android", FakeApi.lastQuery)
    }

    @Test fun `short page ends paging`() = runTest {
        val source = NewsSearchPagingSource(FakeApi { GNewsResponse(20, listOf(article("one"))) }, "android", request)
        val result = source.load(PagingSource.LoadParams.Refresh(null, 10, false)) as PagingSource.LoadResult.Page
        assertEquals(null, result.nextKey)
    }

    @Test fun `network failure becomes paging error`() = runTest {
        val source = NewsSearchPagingSource(FakeApi { throw IOException("offline") }, "android", request)
        val result = source.load(PagingSource.LoadParams.Refresh(null, 10, false))
        assertTrue(result is PagingSource.LoadResult.Error)
    }

    private fun article(id: String) = GNewsArticle(id = id, title = id, url = "https://example.com/$id", source = GNewsSource("source"))

    private class FakeApi(private val handler: suspend () -> GNewsResponse) : GNewsApi {
        companion object { var lastQuery: String? = null }
        override suspend fun topHeadlines(country: String, language: String, category: String?, page: Int, max: Int): GNewsResponse = error("not used")
        override suspend fun search(query: String, language: String, country: String, page: Int, max: Int): GNewsResponse { lastQuery = query; return handler() }
    }
}

