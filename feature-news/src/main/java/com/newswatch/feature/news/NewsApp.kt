package com.newswatch.feature.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import java.util.concurrent.ConcurrentHashMap
import com.newswatch.core.model.Article
import com.newswatch.core.model.Bookmark
import com.newswatch.core.repository.BookmarkRepository
import com.newswatch.core.repository.NewsRepository
import com.newswatch.feature.news.detail.*

private object ArticleSelectionStore {
    private val selected = ConcurrentHashMap<String, Article>()
    fun put(article: Article) { selected[article.id] = article }
    fun get(id: String): Article? = selected[id]
}

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val BOOKMARKS = "bookmarks"
    const val DETAIL = "detail/{id}"
    fun detail(id: String) = "detail/${android.net.Uri.encode(id)}"
}

@Composable
fun NewsWatchApp(news: NewsRepository, bookmarks: BookmarkRepository, starter: ActivityStarter) {
    val nav = rememberNavController()
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar {
            listOf(Routes.HOME to "Home", Routes.SEARCH to "Search", Routes.BOOKMARKS to "Bookmarks").forEach { (route, label) ->
                NavigationBarItem(
                    selected = currentRoute == route,
                    onClick = { nav.navigate(route) { launchSingleTop = true; restoreState = true } },
                    icon = { Icon(if (route == Routes.HOME) Icons.Default.Home else if (route == Routes.SEARCH) Icons.Default.Search else Icons.Default.Bookmark, label) },
                    label = { Text(label) }
                )
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) { FeedScreen(news, nav) }
            composable(Routes.SEARCH) { SearchScreen(news, nav) }
            composable(Routes.BOOKMARKS) { BookmarksScreen(bookmarks, nav) }
            composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
                DetailScreen(news, bookmarks, starter, entry.arguments?.getString("id").orEmpty(), ArticleSelectionStore.get(entry.arguments?.getString("id").orEmpty()))
            }
        }
    }
}

@Composable
private fun FeedScreen(news: NewsRepository, nav: NavHostController) {
    val vm: FeedViewModel = viewModel(factory = NewsViewModelFactory { FeedViewModel(news) })
    val selected by vm.selectedCategory.collectAsState(initial = null)
    val items = vm.articles.collectAsLazyPagingItems()
    Column {
        CategoryRow(selected, vm::selectCategory)
        PagingArticleList("Top headlines", items, nav, "No headlines are cached yet.")
    }
}

@Composable
private fun CategoryRow(selected: String?, onSelected: (String?) -> Unit) {
    val categories = listOf(null to "All", "general" to "General", "business" to "Business", "technology" to "Technology", "sports" to "Sports", "health" to "Health")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { (value, label) ->
            FilterChip(selected = selected == value, onClick = { onSelected(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun SearchScreen(news: NewsRepository, nav: NavHostController) {
    val vm: SearchViewModel = viewModel(factory = NewsViewModelFactory { SearchViewModel(news) })
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) { vm.setQuery(query) }
    val items = vm.results.collectAsLazyPagingItems()
    Column {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp).semantics { contentDescription = "Search news query" }, label = { Text("Search news") }, singleLine = true)
        if (query.trim().length < 2) MessageCard("Enter at least 2 characters to search news.")
        else PagingArticleList("Search results", items, nav, "No matching news was found.")
    }
}

@Composable
private fun PagingArticleList(title: String, items: LazyPagingItems<Article>, nav: NavHostController, emptyMessage: String) {
    val refresh = items.loadState.refresh
    val append = items.loadState.append
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = items::refresh, enabled = refresh !is LoadState.Loading) { Text("Refresh") }
        }
        when {
            refresh is LoadState.Loading && items.itemCount == 0 -> LoadingMessage()
            refresh is LoadState.Error && items.itemCount == 0 -> ErrorMessage("Could not load content.", refresh.error, items::retry)
            refresh is LoadState.NotLoading && items.itemCount == 0 -> MessageCard(emptyMessage)
            else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items.itemCount, key = { index -> items[index]?.id ?: "placeholder-$index" }, contentType = { "article" }) { index ->
                    items[index]?.let { ArticleRow(it) { openDetail(nav, it) } }
                }
                when (append) {
                    is LoadState.Loading -> item { LoadingMessage("Loading more…") }
                    is LoadState.Error -> item { ErrorMessage("Could not load more articles.", append.error, items::retry) }
                    else -> Unit
                }
            }
        }
    }
}

@Composable private fun LoadingMessage(text: String = "Loading…") { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator(Modifier.semantics { contentDescription = text }) } }

@Composable private fun ErrorMessage(message: String, error: Throwable, retry: () -> Unit) { Card(Modifier.fillMaxWidth().padding(16.dp)) { Column(Modifier.padding(16.dp)) { Text(message); Text(error.message ?: "Please try again.", style = MaterialTheme.typography.bodySmall); Button(onClick = retry, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") } } } }

@Composable private fun MessageCard(message: String) { Card(Modifier.fillMaxWidth().padding(16.dp)) { Text(message, Modifier.padding(16.dp)) } }

@Composable
private fun BookmarksScreen(repo: BookmarkRepository, nav: NavHostController) {
    val vm: BookmarksViewModel = viewModel(factory = NewsViewModelFactory { BookmarksViewModel(repo) })
    var query by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(query) { vm.setQuery(query) }
    val items by vm.bookmarks.collectAsState(initial = emptyList())
    Column {
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(12.dp).semantics { contentDescription = "Search bookmarks query" }, label = { Text("Search bookmarks") }, singleLine = true)
        if (items.isEmpty()) MessageCard(if (query.isBlank()) "No bookmarks yet. Save an article to read it offline." else "No matching bookmarks found.")
        else LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(items, key = { it.article.id }) { bookmark -> ArticleRow(bookmark.article) { openDetail(nav, bookmark.article) } } }
    }
}

private fun openDetail(nav: NavHostController, article: Article) { ArticleSelectionStore.put(article); nav.navigate(Routes.detail(article.id)) }

@Composable private fun ArticleRow(article: Article, onClick: () -> Unit) { ListItem(headlineContent = { Text(article.title) }, supportingContent = { Text(article.sourceName) }, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).semantics { contentDescription = "Open article ${article.title}" }) }

@Composable
private fun DetailScreen(news: NewsRepository, bookmarks: BookmarkRepository, starter: ActivityStarter, id: String, selectedArticle: Article?) {
    val vm: DetailViewModel = viewModel(key = id, factory = NewsViewModelFactory { DetailViewModel(news, bookmarks, id, selectedArticle) })
    val article by vm.article.collectAsState(initial = null)
    val saved by vm.isBookmarked.collectAsState(initial = false)
    article?.let { a ->
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(a.title, style = MaterialTheme.typography.headlineSmall)
            Text(a.sourceName, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(16.dp))
            Text(a.description ?: a.content ?: "No additional content available.")
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton({ vm.toggle(a, saved) }) { Text(if (saved) "Remove bookmark" else "Bookmark") }
                TextButton({ ArticleActionLauncher(starter).openOriginal(a) }) { Text("Read original") }
                TextButton({ ArticleActionLauncher(starter).share(a) }) { Text("Share") }
            }
        }
    } ?: MessageCard("This article is unavailable. It may no longer be cached or bookmarked.")
}






