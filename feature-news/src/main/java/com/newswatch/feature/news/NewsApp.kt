package com.newswatch.feature.news

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
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
                DetailScreen(news, bookmarks, starter, nav, entry.arguments?.getString("id").orEmpty(), ArticleSelectionStore.get(entry.arguments?.getString("id").orEmpty()))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen(news: NewsRepository, nav: NavHostController) {
    val vm: FeedViewModel = viewModel(factory = NewsViewModelFactory { FeedViewModel(news) })
    val selected by vm.selectedCategory.collectAsState(initial = null)
    val items = vm.articles.collectAsLazyPagingItems()
    Column {
        CategoryRow(selected, vm::selectCategory)
        PullToRefreshBox(isRefreshing = items.loadState.refresh is LoadState.Loading && items.itemCount > 0, onRefresh = items::refresh, state = rememberPullToRefreshState()) { PagingArticleList("Top headlines", items, nav, "No headlines are cached yet.") }
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
    val items = vm.results.collectAsLazyPagingItems()
    Column {
        OutlinedTextField(value = query, onValueChange = { query = it; vm.setQuery(it) }, modifier = Modifier.fillMaxWidth().padding(12.dp).semantics { contentDescription = "Search news query" }, label = { Text("Search news") }, singleLine = true)
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
                    is LoadState.Error -> item { InlineAppendError(items::retry) }
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun LoadingMessage(text: String = "Loading…") {
    val transition = rememberInfiniteTransition(label = "loading-shimmer")
    val offset by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Restart), label = "shimmer-offset")
    val brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant), start = androidx.compose.ui.geometry.Offset(offset * 500f, 0f), end = androidx.compose.ui.geometry.Offset(offset * 500f + 300f, 0f))
    Column(Modifier.fillMaxWidth().padding(16.dp).semantics { contentDescription = text }, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { Row(Modifier.fillMaxWidth().height(92.dp).background(brush, MaterialTheme.shapes.medium).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Box(Modifier.size(68.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), MaterialTheme.shapes.small)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.fillMaxWidth().height(16.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))); Box(Modifier.fillMaxWidth(0.7f).height(12.dp).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))) } }
        }
        CircularProgressIndicator(Modifier.size(24.dp).align(androidx.compose.ui.Alignment.CenterHorizontally))
    }
}

@Composable private fun ErrorMessage(message: String, error: Throwable, retry: () -> Unit) { Card(Modifier.fillMaxWidth().padding(16.dp)) { Column(Modifier.padding(16.dp)) { Text(message); Text(error.message ?: "Please try again.", style = MaterialTheme.typography.bodySmall); Button(onClick = retry, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") } } } }

@Composable private fun InlineAppendError(retry: () -> Unit) { Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.Center) { Text("Could not load more"); TextButton(onClick = retry) { Text("Retry") } } }

@Composable private fun PlaceholderImage(modifier: Modifier) { Box(modifier.clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = androidx.compose.ui.Alignment.Center) { Icon(Icons.Default.Image, contentDescription = "Article image unavailable", tint = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun MessageCard(message: String) { Card(Modifier.fillMaxWidth().padding(16.dp)) { Text(message, Modifier.padding(16.dp)) } }

private fun formatPublishedAt(value: String?): String = value?.take(10) ?: "Unknown date"

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

@Composable
private fun ArticleRow(article: Article, onClick: () -> Unit) {
    AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 8 })) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick).semantics { contentDescription = "Open article ${article.title}" }) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (article.imageUrl.isNullOrBlank()) PlaceholderImage(Modifier.size(96.dp)) else AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(article.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.medium), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(article.description ?: "No summary available.", style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(article.sourceName, style = MaterialTheme.typography.labelSmall); Text(formatPublishedAt(article.publishedAt), style = MaterialTheme.typography.labelSmall) }
            }
        }
    }
}

    }
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(news: NewsRepository, bookmarks: BookmarkRepository, starter: ActivityStarter, nav: NavHostController, id: String, selectedArticle: Article?) {
    val vm: DetailViewModel = viewModel(key = id, factory = NewsViewModelFactory { DetailViewModel(news, bookmarks, id, selectedArticle) })
    val article by vm.article.collectAsState(initial = null)
    val saved by vm.isBookmarked.collectAsState(initial = false)
    Scaffold(topBar = { TopAppBar(title = { Text("Article") }, navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }, actions = { IconButton({ article?.let { ArticleActionLauncher(starter).share(it) } }, enabled = article != null) { Icon(Icons.Default.Share, "Share") } }) }) { padding ->
        article?.let { a ->
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (a.imageUrl.isNullOrBlank()) PlaceholderImage(Modifier.fillMaxWidth().height(200.dp)) else AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(a.imageUrl).crossfade(true).build(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp).clip(MaterialTheme.shapes.medium), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Text(a.title, style = MaterialTheme.typography.headlineSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(a.sourceName, style = MaterialTheme.typography.labelLarge); Text(formatPublishedAt(a.publishedAt), style = MaterialTheme.typography.labelLarge) }
                Text(a.description ?: a.content ?: "No additional content available.", style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton({ vm.toggle(a, saved) }) { Icon(if (saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, "Bookmark"); Spacer(Modifier.width(4.dp)); Text(if (saved) "Saved" else "Save") }
                    OutlinedButton({ ArticleActionLauncher(starter).openOriginal(a) }) { Icon(Icons.Default.OpenInBrowser, "Open original"); Spacer(Modifier.width(4.dp)); Text("Read original") }
                }
            }
        } ?: Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { MessageCard("This article is unavailable. It may no longer be cached or bookmarked.") }
    }
}

