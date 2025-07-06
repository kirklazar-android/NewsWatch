package com.newswatch.data.network

import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GNewsParsingTest {
    private val adapter = Moshi.Builder().build().adapter(GNewsResponse::class.java)

    @Test fun `parses complete response`() {
        val response = adapter.fromJson("""{"totalArticles":1,"articles":[{"title":"Headline","description":"Summary","url":"https://example.com/a","image":null,"publishedAt":"2026-07-24T00:00:00Z","source":{"name":"Example"}}]}""")!!
        assertEquals(1, response.totalArticles)
        assertEquals("Headline", response.articles.single().title)
        assertNull(response.articles.single().image)
    }

    @Test fun `parses empty response`() {
        val response = adapter.fromJson("""{"totalArticles":0,"articles":[]}""")!!
        assertEquals(emptyList<GNewsArticle>(), response.articles)
    }

    @Test(expected = JsonDataException::class)
    fun `rejects malformed field types`() { adapter.fromJson("""{"totalArticles":"bad","articles":[]}""") }
}