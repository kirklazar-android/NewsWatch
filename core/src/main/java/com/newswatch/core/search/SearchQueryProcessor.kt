package com.newswatch.core.search

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.paging.PagingData
import com.newswatch.core.model.Article
import com.newswatch.core.model.FeedRequest
import com.newswatch.core.repository.NewsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchQueryProcessor(
    private val repository: NewsRepository,
    private val debounceMillis: Long = DEFAULT_DEBOUNCE_MILLIS,
    private val minimumQueryLength: Int = MINIMUM_QUERY_LENGTH,
) {
    fun observe(
        queries: Flow<String>,
        request: FeedRequest,
        isOnline: Flow<Boolean>,
    ): Flow<PagingData<Article>> = queries
        .map { it.trim().replace(Regex("\\s+"), " ").lowercase() }
        .debounce(debounceMillis)
        .distinctUntilChanged()
        .combine(isOnline) { query, online -> query to online }
        .flatMapLatest { (query, online) ->
            if (!online || query.length < minimumQueryLength) flowOf(PagingData.empty())
            else repository.search(query, request)
        }

    companion object {
        const val DEFAULT_DEBOUNCE_MILLIS = 300L
        const val MINIMUM_QUERY_LENGTH = 2
    }
}