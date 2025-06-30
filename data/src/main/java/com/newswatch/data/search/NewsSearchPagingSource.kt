package com.newswatch.data.search

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.newswatch.core.model.Article
import com.newswatch.core.model.FeedRequest
import com.newswatch.data.network.GNewsApi
import com.newswatch.data.network.GNewsConfig
import com.newswatch.data.network.toDomain
import com.newswatch.data.network.toNewsError
import com.newswatch.data.paging.NewsPagingException

class NewsSearchPagingSource(
    private val api: GNewsApi,
    private val query: String,
    private val request: FeedRequest,
) : PagingSource<Int, Article>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Article> {
        val page = params.key ?: 1
        return try {
            val response = api.search(query, request.language, request.country, page, GNewsConfig.FREE_PLAN_PAGE_SIZE)
            val articles = response.articles.mapIndexed { index, article ->
                article.toDomain((page - 1) * GNewsConfig.FREE_PLAN_PAGE_SIZE + index)
            }
            val endReached = articles.isEmpty() ||
                articles.size < GNewsConfig.FREE_PLAN_PAGE_SIZE ||
                page * GNewsConfig.FREE_PLAN_PAGE_SIZE >= response.totalArticles
            LoadResult.Page(
                data = articles,
                prevKey = page.takeIf { it > 1 }?.minus(1),
                nextKey = page.plus(1).takeUnless { endReached },
            )
        } catch (error: Throwable) {
            LoadResult.Error(if (error is IllegalArgumentException) NewsPagingException(com.newswatch.core.model.NewsError.Validation) else NewsPagingException(error.toNewsError()))
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Article>): Int? = state.anchorPosition?.let { anchor ->
        state.closestPageToPosition(anchor)?.prevKey?.plus(1)
            ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
    }
}