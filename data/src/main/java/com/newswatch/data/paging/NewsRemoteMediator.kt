package com.newswatch.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType

import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.newswatch.core.model.FeedRequest
import com.newswatch.core.model.NewsError
import com.newswatch.data.network.GNewsApi
import com.newswatch.data.network.GNewsConfig
import com.newswatch.data.network.toDomain
import com.newswatch.data.network.toNewsError
import com.newswatch.data.persistence.CachedArticleEntity
import com.newswatch.data.persistence.FeedMetadataEntity
import com.newswatch.data.persistence.NewsDatabase
import com.newswatch.data.persistence.toCachedEntity
import java.io.IOException

class NewsPagingException(val error: NewsError) : IOException(error.toString())

@OptIn(ExperimentalPagingApi::class)
class NewsRemoteMediator(
    private val request: FeedRequest,
    private val api: GNewsApi,
    private val database: NewsDatabase,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) : RemoteMediator<Int, CachedArticleEntity>() {
    override suspend fun initialize(): InitializeAction {
        database.trimFeed()
        val metadata = database.feedMetadataDao().get()
        val cacheCount = database.cachedArticleDao().count()
        val refreshIsFresh = metadata?.requestKey == request.key &&
            metadata.lastSuccessfulRefresh != null &&
            nowMillis() - metadata.lastSuccessfulRefresh < REFRESH_INTERVAL_MILLIS
        return if (cacheCount > 0 && refreshIsFresh) InitializeAction.SKIP_INITIAL_REFRESH
        else InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, CachedArticleEntity>): RemoteMediator.MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return RemoteMediator.MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> database.feedMetadataDao().get()?.let { metadata ->
                if (metadata.requestKey == request.key && !metadata.endOfPagination) metadata.nextPage else null
            } ?: return RemoteMediator.MediatorResult.Success(endOfPaginationReached = true)
        }

        return try {
            val response = api.topHeadlines(
                country = request.country,
                language = request.language,
                category = request.category,
                page = page,
                max = GNewsConfig.FREE_PLAN_PAGE_SIZE,
            )
            val positionStart = (page - 1) * GNewsConfig.FREE_PLAN_PAGE_SIZE
            val articles = response.articles.mapIndexed { index, article ->
                try {
                    article.toDomain(positionStart + index).toCachedEntity(nowMillis())
                } catch (error: IllegalArgumentException) {
                    throw NewsPagingException(NewsError.Validation)
                }
            }
            val endReached = articles.isEmpty() ||
                articles.size < GNewsConfig.FREE_PLAN_PAGE_SIZE ||
                page * GNewsConfig.FREE_PLAN_PAGE_SIZE >= response.totalArticles
            val metadata = FeedMetadataEntity(
                requestKey = request.key,
                nextPage = page + 1,
                endOfPagination = endReached,
                lastSuccessfulRefresh = if (loadType == LoadType.REFRESH) nowMillis() else database.feedMetadataDao().get()?.lastSuccessfulRefresh,
            )
            if (loadType == LoadType.REFRESH) database.replaceFeed(articles, metadata)
            else database.appendFeed(articles, metadata)
            RemoteMediator.MediatorResult.Success(endReached)
        } catch (error: NewsPagingException) {
            RemoteMediator.MediatorResult.Error(error)
        } catch (error: Throwable) {
            RemoteMediator.MediatorResult.Error(NewsPagingException(error.toNewsError()))
        }
    }

    companion object { const val REFRESH_INTERVAL_MILLIS = 30 * 60 * 1000L }
}