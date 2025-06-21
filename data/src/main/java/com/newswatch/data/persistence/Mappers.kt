package com.newswatch.data.persistence

import com.newswatch.core.model.Article
import com.newswatch.core.model.Bookmark

fun Article.toCachedEntity(cachedAt: Long): CachedArticleEntity = CachedArticleEntity(
    id, title, description, content, sourceName, imageUrl, originalUrl, publishedAt,
    feedPosition ?: error("Cached feed articles require feedPosition"), cachedAt,
)

fun CachedArticleEntity.toDomain(): Article = Article(id, title, description, content, sourceName, imageUrl, originalUrl, publishedAt, feedPosition)

fun Article.toBookmarkedEntity(bookmarkedAt: Long): BookmarkedArticleEntity = BookmarkedArticleEntity(
    id, title, description, content, sourceName, imageUrl, originalUrl, publishedAt, bookmarkedAt,
)

fun BookmarkedArticleEntity.toDomain(): Bookmark = Bookmark(
    Article(id, title, description, content, sourceName, imageUrl, originalUrl, publishedAt), bookmarkedAt,
)