package com.newswatch.data.network

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        check(apiKey.isNotBlank()) { "GNews API key is missing. Configure GNEWS_API_KEY locally." }
        val request = chain.request().newBuilder()
            .url(chain.request().url.newBuilder().addQueryParameter("apikey", apiKey).build())
            .build()
        return chain.proceed(request)
    }
}