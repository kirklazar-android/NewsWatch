package com.newswatch.data.network

import com.newswatch.core.model.Article
import java.net.URI
import java.security.MessageDigest

fun GNewsArticle.toDomain(position: Int? = null): Article {
    require(title.isNotBlank()) { "Article title is required" }
    require(url.isNotBlank()) { "Article URL is required" }
    val canonicalUrl = normalizeUrl(url)
    val stableId = id?.takeIf(String::isNotBlank) ?: canonicalUrl.sha256()
    return Article(stableId, title.trim(), description, content, source.name.ifBlank { "Unknown" }, image, canonicalUrl, publishedAt, position)
}

internal fun normalizeUrl(value: String): String {
    val uri = URI(value.trim())
    val scheme = uri.scheme?.lowercase() ?: "https"
    val host = uri.host?.lowercase() ?: throw IllegalArgumentException("Article URL must include a host")
    val path = uri.path?.ifBlank { "/" } ?: "/"
    return URI(scheme, uri.userInfo, host, uri.port, path, uri.query, null).toASCIIString()
}

private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
    .digest(toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }