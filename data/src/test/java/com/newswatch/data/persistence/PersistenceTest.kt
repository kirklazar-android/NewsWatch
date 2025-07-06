package com.newswatch.data.persistence

import android.content.Context
import androidx.room.Room
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner
import com.newswatch.core.model.Article
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class PersistenceTest {
    private lateinit var database: NewsDatabase
    private lateinit var context: Context

    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, NewsDatabase::class.java).allowMainThreadQueries().build()
    }

    @After fun tearDown() = database.close()

    @Test fun `replacement atomically leaves only new feed and metadata`() = runTest {
        val old = article("old", 0)
        val next = article("next", 0)
        database.replaceFeed(listOf(old), metadata("old-request", 2))
        database.replaceFeed(listOf(next), metadata("new-request", 2))

        assertEquals(listOf("next"), database.cachedArticleDao().all().map { it.id })
        assertEquals("new-request", database.feedMetadataDao().get()?.requestKey)
    }

    @Test fun `append preserves order and metadata`() = runTest {
        database.replaceFeed(listOf(article("a", 0)), metadata("request", 2))
        database.appendFeed(listOf(article("b", 1)), metadata("request", 3))

        assertEquals(listOf("a", "b"), database.cachedArticleDao().all().map { it.id })
        assertEquals(3, database.feedMetadataDao().get()?.nextPage)
    }

    @Test fun `startup trim keeps first 150 ranked articles`() = runTest {
        database.replaceFeed((0 until 200).map { article("article-$it", it) }, metadata("request", 21))
        database.trimFeed()

        val rows = database.cachedArticleDao().all()
        assertEquals(150, rows.size)
        assertEquals(0, rows.first().feedPosition)
        assertEquals(149, rows.last().feedPosition)
    }

    @Test fun `bookmarks survive feed replacement and support search and state`() = runTest {
        val article = Article("bookmark", "Saved Headline", "description", "content", "Source", null, "https://example.com/saved", null)
        database.bookmarkDao().upsert(article.toBookmarkedEntity(1L))
        database.replaceFeed(listOf(article("feed", 0)), metadata("request", 2))
        database.trimFeed()

        assertEquals(1, database.bookmarkDao().count())
        assertTrue(database.bookmarkDao().isBookmarked("bookmark").first())
        assertEquals(1, database.bookmarkDao().search("headline").first().size)
        database.bookmarkDao().delete("bookmark")
        assertFalse(database.bookmarkDao().isBookmarked("bookmark").first())
    }

    private fun article(id: String, position: Int) = CachedArticleEntity(id, id, null, null, "Source", null, "https://example.com/$id", null, position, 1L)
    private fun metadata(request: String, nextPage: Int) = FeedMetadataEntity(requestKey = request, nextPage = nextPage, endOfPagination = false, lastSuccessfulRefresh = 1L)
}