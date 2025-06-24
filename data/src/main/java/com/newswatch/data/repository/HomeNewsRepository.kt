package com.newswatch.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.newswatch.core.model.Article
import com.newswatch.core.model.FeedRequest
import com.newswatch.core.repository.NewsRepository
import com.newswatch.data.network.GNewsApi
import com.newswatch.data.network.GNewsConfig
import com.newswatch.data.persistence.NewsDatabase
import com.newswatch.data.persistence.toDomain
import com.newswatch.data.paging.NewsRemoteMediator
import com.newswatch.data.search.NewsSearchPagingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalPagingApi::class)
class HomeNewsRepository(
    private val api: GNewsApi,
    private val database: NewsDatabase,
) : NewsRepository {
    override fun observeFeed(request: FeedRequest): Flow<PagingData<Article>> = Pager(
        config = PagingConfig(pageSize = GNewsConfig.FREE_PLAN_PAGE_SIZE, enablePlaceholders = false),
        remoteMediator = NewsRemoteMediator(request, api, database),
        pagingSourceFactory = { database.cachedArticleDao().pagingSource() },
    ).flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }

    override fun search(query: String, request: FeedRequest): Flow<PagingData<Article>> = Pager(
        config = PagingConfig(pageSize = GNewsConfig.FREE_PLAN_PAGE_SIZE, enablePlaceholders = false),
        pagingSourceFactory = { NewsSearchPagingSource(api, query.trim(), request) },
    ).flow

    override suspend fun getArticle(id: String): Article? = database.cachedArticleDao().getById(id)?.toDomain()
}