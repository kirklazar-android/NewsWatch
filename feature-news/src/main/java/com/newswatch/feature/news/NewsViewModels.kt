package com.newswatch.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.newswatch.core.model.Article
import com.newswatch.core.model.Bookmark
import com.newswatch.core.model.FeedRequest
import com.newswatch.core.repository.BookmarkRepository
import com.newswatch.core.repository.NewsRepository
import com.newswatch.core.search.SearchQueryProcessor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(private val repository: NewsRepository) : ViewModel() {
    private val request = MutableStateFlow(FeedRequest())
    val selectedCategory: Flow<String?> = request.map { it.category }
    val articles: Flow<PagingData<Article>> = request
        .flatMapLatest(repository::observeFeed)
        .cachedIn(viewModelScope)
    fun selectCategory(category: String?) { request.value = FeedRequest.normalized(category = category) }
}

class SearchViewModel(private val repository: NewsRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val online = MutableStateFlow(true)
    private val processor = SearchQueryProcessor(repository)
    val results: Flow<PagingData<Article>> = processor.observe(query, FeedRequest(), online).cachedIn(viewModelScope)
    fun setQuery(value: String) { query.value = value }
    fun setOnline(value: Boolean) { online.value = value }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarksViewModel(private val repository: BookmarkRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val bookmarks: Flow<List<Bookmark>> = query.flatMapLatest { q ->
        if (q.isBlank()) repository.observeBookmarks() else repository.searchBookmarks(q.trim())
    }
    fun setQuery(value: String) { query.value = value }
}

class DetailViewModel(private val news: NewsRepository, private val bookmarks: BookmarkRepository, private val id: String, private val selectedArticle: Article? = null) : ViewModel() {
    val article: Flow<Article?> = kotlinx.coroutines.flow.flow { emit(selectedArticle ?: news.getArticle(id) ?: bookmarks.getBookmark(id)?.article) }
    val isBookmarked = bookmarks.isBookmarked(id)
    fun toggle(article: Article, bookmarked: Boolean) = viewModelScope.launch {
        if (bookmarked) bookmarks.removeBookmark(article.id) else bookmarks.saveBookmark(article)
    }
}

class NewsViewModelFactory(private val create: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = create() as T
}



