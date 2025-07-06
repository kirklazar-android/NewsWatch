package com.newswatch.data.paging

import android.content.Context
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.room.Room
import com.newswatch.core.model.FeedRequest
import com.newswatch.data.network.GNewsApi
import com.newswatch.data.network.GNewsArticle
import com.newswatch.data.network.GNewsResponse
import com.newswatch.data.network.GNewsSource
import com.newswatch.data.persistence.FeedMetadataEntity
import com.newswatch.data.persistence.NewsDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class NewsRemoteMediatorTest {
    private lateinit var database: NewsDatabase
    private lateinit var context: Context
    private val request = FeedRequest()
    private val state = PagingState<Int, com.newswatch.data.persistence.CachedArticleEntity>(emptyList(), null, PagingConfig(10), 0)

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, NewsDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() = database.close()

    @Test fun `refresh writes ranked page and metadata`() = runTest {
        val mediator = mediator(FakeApi { page -> response(page, 10) }, now = 1_000L)
        val result = mediator.load(LoadType.REFRESH, state)

        assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
        assertEquals((0 until 10).toList(), database.cachedArticleDao().all().map { it.feedPosition })
        assertEquals(2, database.feedMetadataDao().get()?.nextPage)
        assertEquals(request.key, database.feedMetadataDao().get()?.requestKey)
    }

    @Test fun `append adds next page and marks end when response is short`() = runTest {
        val mediator = mediator(FakeApi { page -> if (page == 1) response(page, 10) else response(page, 2) }, now = 1_000L)
        mediator.load(LoadType.REFRESH, state)
        val result = mediator.load(LoadType.APPEND, state)

        assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Success)
        assertEquals(12, database.cachedArticleDao().count())
        assertEquals(true, database.feedMetadataDao().get()?.endOfPagination)
    }

    @Test fun `refresh failure preserves previous cache and metadata`() = runTest {
        val first = mediator(FakeApi { response(1, 1) }, now = 1_000L)
        first.load(LoadType.REFRESH, state)
        val before = database.cachedArticleDao().all()
        val beforeMetadata = database.feedMetadataDao().get()
        val failing = mediator(FakeApi { throw IOException("offline") }, now = 2_000L)

        val result = failing.load(LoadType.REFRESH, state)

        assertTrue(result is androidx.paging.RemoteMediator.MediatorResult.Error)
        assertEquals(before, database.cachedArticleDao().all())
        assertEquals(beforeMetadata, database.feedMetadataDao().get())
    }

    @Test fun `request key change launches refresh and replaces old feed only after success`() = runTest {
        val first = mediator(FakeApi { response(1, 1) }, now = 1_000L)
        first.load(LoadType.REFRESH, state)
        val newRequest = FeedRequest.normalized(category = "technology")
        val failing = NewsRemoteMediator(newRequest, FakeApi { throw IOException("offline") }, database) { 2_000L }
        assertEquals(androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, failing.initialize())
        failing.load(LoadType.REFRESH, state)
        assertEquals(listOf("article-1-0"), database.cachedArticleDao().all().map { it.id })
        assertEquals(request.key, database.feedMetadataDao().get()?.requestKey)
    }

    @Test fun `prepend is always end of pagination`() = runTest {
        val result = mediator(FakeApi { response(1, 1) }, now = 1_000L).load(LoadType.PREPEND, state)
        assertEquals(true, (result as androidx.paging.RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test fun `stale matching cache launches initial refresh`() = runTest {
        mediator(FakeApi { response(1, 1) }, now = 1_000L).load(LoadType.REFRESH, state)
        val stale = NewsRemoteMediator(request, FakeApi { response(1, 1) }, database) { 1_000L + NewsRemoteMediator.REFRESH_INTERVAL_MILLIS }
        assertEquals(androidx.paging.RemoteMediator.InitializeAction.LAUNCH_INITIAL_REFRESH, stale.initialize())
    }

    @Test fun `fresh matching cache skips initial refresh`() = runTest {
        mediator(FakeApi { response(1, 1) }, now = 1_000L).load(LoadType.REFRESH, state)
        val fresh = NewsRemoteMediator(request, FakeApi { response(1, 1) }, database) { 1_000L + 1_000L }
        assertEquals(androidx.paging.RemoteMediator.InitializeAction.SKIP_INITIAL_REFRESH, fresh.initialize())
    }

    private fun mediator(api: GNewsApi, now: Long) = NewsRemoteMediator(request, api, database) { now }

    private fun response(page: Int, count: Int) = GNewsResponse(
        totalArticles = 100,
        articles = (0 until count).map { index ->
            GNewsArticle(id = "article-$page-$index", title = "Headline $page-$index", url = "https://example.com/$page-$index", source = GNewsSource("Source"))
        },
    )

    private class FakeApi(private val handler: suspend (Int) -> GNewsResponse) : GNewsApi {
        override suspend fun topHeadlines(country: String, language: String, category: String?, page: Int, max: Int): GNewsResponse = handler(page)
        override suspend fun search(query: String, language: String, country: String, page: Int, max: Int): GNewsResponse = error("not used")
    }
}