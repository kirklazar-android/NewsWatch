package com.newswatch.data.network

import com.newswatch.core.model.NewsError
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class GNewsContractTest {
    private lateinit var server: MockWebServer
    private lateinit var api: GNewsApi

    @Before fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor("test-key")).build())
            .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().build()))
            .build()
            .create(GNewsApi::class.java)
    }

    @After fun tearDown() = server.shutdown()

    @Test fun `defaults are India English and page size ten`() {
        assertEquals("in", GNewsConfig.DEFAULT_COUNTRY)
        assertEquals("en", GNewsConfig.DEFAULT_LANGUAGE)
        assertEquals(10, GNewsConfig.DEFAULT_PAGE_SIZE)
    }

    @Test fun `top headlines request includes provider contract`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"totalArticles":0,"articles":[]}"""))
        api.topHeadlines("IN", "en", "technology", 2, 10)
        val request = server.takeRequest()
        assertEquals("/top-headlines", request.requestUrl?.encodedPath)
        assertEquals("test-key", request.requestUrl?.queryParameter("apikey"))
        assertEquals("IN", request.requestUrl?.queryParameter("country"))
        assertEquals("en", request.requestUrl?.queryParameter("lang"))
        assertEquals("technology", request.requestUrl?.queryParameter("category"))
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("10", request.requestUrl?.queryParameter("max"))
    }

    @Test fun `search request includes query and omits category`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"totalArticles":0,"articles":[]}"""))
        api.search("android", "en", "IN", 1, 10)
        val url = server.takeRequest().requestUrl!!
        assertEquals("/search", url.encodedPath)
        assertEquals("android", url.queryParameter("q"))
        assertFalse(url.queryParameterNames.contains("category"))
    }

    @Test fun `article mapper creates stable normalized fallback id`() {
        val first = GNewsArticle(title = "A", url = "HTTPS://Example.COM/a#section").toDomain()
        val second = GNewsArticle(title = "A", url = "https://example.com/a").toDomain()
        assertEquals(first.id, second.id)
        assertNotEquals("", first.id)
        assertEquals("https://example.com/a", first.originalUrl)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `article mapper rejects blank URL`() { GNewsArticle(title = "A", url = "").toDomain() }

    @Test fun `authentication failures are classified separately`() {
        val body = "{}".toResponseBody("application/json".toMediaType())
        assertEquals(NewsError.Authentication, HttpException(Response.error<Any>(401, body)).toNewsError())
        assertEquals(NewsError.Authentication, HttpException(Response.error<Any>(403, body)).toNewsError())
    }
    @Test fun `quota is classified separately`() {
        val body = "{}".toResponseBody("application/json".toMediaType())
        val failure = HttpException(Response.error<Any>(429, body))
        assertEquals(NewsError.Quota, failure.toNewsError())
    }
}