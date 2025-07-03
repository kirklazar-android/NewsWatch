package com.newswatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.newswatch.core.repository.BookmarkRepository
import com.newswatch.core.repository.NewsRepository
import com.newswatch.feature.news.NewsWatchApp
import com.newswatch.feature.news.detail.AndroidActivityStarter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var newsRepository: NewsRepository
    @Inject lateinit var bookmarkRepository: BookmarkRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { NewsWatchApp(newsRepository, bookmarkRepository, AndroidActivityStarter(this)) } }
    }
}
