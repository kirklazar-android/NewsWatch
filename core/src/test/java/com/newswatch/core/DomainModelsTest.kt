package com.newswatch.core

import com.newswatch.core.model.Article
import com.newswatch.core.model.FeedRequest
import com.newswatch.core.model.NewsError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {
    @Test fun `defaults use India English and all category key`() {
        val request = FeedRequest()
        assertEquals("in", request.country)
        assertEquals("en", request.language)
        assertEquals("in:en:all", request.key)
    }

    @Test fun `normalized request trims lowercases and removes blank category`() {
        val request = FeedRequest.normalized(" IN ", " EN ", "  ")
        assertEquals(FeedRequest(), request)
    }

    @Test fun `request keys distinguish scope changes`() {
        val base = FeedRequest()
        assertFalse(base.key == FeedRequest.normalized(category = "technology").key)
        assertFalse(base.key == FeedRequest.normalized(country = "us").key)
        assertFalse(base.key == FeedRequest.normalized(language = "hi").key)
        assertEquals(base.key, FeedRequest.normalized("in", "en", null).key)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `direct construction rejects unnormalized country`() { FeedRequest(country = " IN ") }

    @Test fun `article identity remains domain data`() {
        val article = Article("stable", "Title", null, null, "Source", null, "https://example.com", null)
        assertEquals("stable", article.id)
        assertEquals("https://example.com", article.originalUrl)
    }

    @Test fun `errors expose retry policy`() {
        assertTrue(NewsError.Network.retryable)
        assertTrue(NewsError.Http(500).retryable)
        assertFalse(NewsError.Http(401).retryable)
        assertFalse(NewsError.Authentication.retryable)
        assertFalse(NewsError.Quota.retryable)
        assertFalse(NewsError.Validation.retryable)
    }
}
