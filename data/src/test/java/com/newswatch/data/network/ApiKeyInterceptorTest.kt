package com.newswatch.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test

class ApiKeyInterceptorTest {
    @Test(expected = IllegalStateException::class)
    fun `blank API key fails before network request`() {
        val server = MockWebServer().apply { start() }
        try {
            val client = OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor("")).build()
            client.newCall(Request.Builder().url(server.url("/")).build()).execute()
        } finally { server.shutdown() }
    }
}