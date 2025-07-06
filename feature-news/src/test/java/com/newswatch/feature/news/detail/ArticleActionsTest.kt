package com.newswatch.feature.news.detail

import android.content.Intent
import com.newswatch.core.model.Article
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ArticleActionsTest {
    private val article = Article("a1", "Headline", null, null, "Source", null, "https://example.com/a1", null)

    @Test fun `share launches chooser with article URL`() {
        val starter = RecordingStarter(canHandle = true)
        val result = ArticleActionLauncher(starter).share(article)
        assertEquals(ArticleActionResult.Completed, result)
        assertEquals(Intent.ACTION_CHOOSER, starter.last?.action)
        val send = starter.last?.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
        assertEquals("https://example.com/a1", send?.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test fun `custom tabs is preferred and browser is fallback`() {
        val customTabs = RecordingStarter(canHandle = true)
        assertEquals(ArticleActionResult.Completed, ArticleActionLauncher(customTabs).openOriginal(article))
        assertEquals(Intent.ACTION_VIEW, customTabs.last?.action)

        val browser = RecordingStarter(canHandle = false).apply { fallbackHandle = true }
        assertEquals(ArticleActionResult.Completed, ArticleActionLauncher(browser).openOriginal(article))
        assertEquals(Intent.ACTION_VIEW, browser.last?.action)
    }

    @Test fun `missing URL fails without launching`() {
        val starter = RecordingStarter(canHandle = true)
        val invalid = article.copy(originalUrl = "")
        val result = ArticleActionLauncher(starter).share(invalid)
        assertEquals(ArticleActionResult.Failed(ArticleActionResult.Reason.MissingUrl), result)
        assertTrue(starter.last == null)
    }

    private class RecordingStarter(private val canHandle: Boolean) : ActivityStarter {
        var fallbackHandle = false
        var last: Intent? = null
        override fun canHandle(intent: Intent): Boolean = canHandle || fallbackHandle
        override fun start(intent: Intent) { last = intent }
    }
}