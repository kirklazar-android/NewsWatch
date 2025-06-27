package com.newswatch.feature.news.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.newswatch.core.model.Article

sealed interface ArticleActionResult {
    data object Completed : ArticleActionResult
    data class Failed(val reason: Reason) : ArticleActionResult

    enum class Reason { MissingUrl, NoHandler, LaunchFailed }
}

interface ActivityStarter {
    fun canHandle(intent: Intent): Boolean
    fun start(intent: Intent)
}

class AndroidActivityStarter(private val context: Context) : ActivityStarter {
    override fun canHandle(intent: Intent): Boolean = intent.resolveActivity(context.packageManager) != null
    override fun start(intent: Intent) = context.startActivity(intent)
}

class ArticleActionLauncher(private val starter: ActivityStarter) {
    fun share(article: Article): ArticleActionResult {
        val url = article.originalUrl.takeIf { it.isNotBlank() } ?: return ArticleActionResult.Failed(ArticleActionResult.Reason.MissingUrl)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_TITLE, article.title)
        }
        return launch(Intent.createChooser(send, "Share article"))
    }

    fun openOriginal(article: Article): ArticleActionResult {
        val url = article.originalUrl.takeIf { it.isNotBlank() } ?: return ArticleActionResult.Failed(ArticleActionResult.Reason.MissingUrl)
        val customTabs = CustomTabsIntent.Builder().build().intent.apply { data = Uri.parse(url) }
        if (starter.canHandle(customTabs)) return launch(customTabs)
        return openInBrowser(article)
    }

    fun openInBrowser(article: Article): ArticleActionResult {
        val url = article.originalUrl.takeIf { it.isNotBlank() } ?: return ArticleActionResult.Failed(ArticleActionResult.Reason.MissingUrl)
        val browser = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return launch(browser)
    }

    private fun launch(intent: Intent): ArticleActionResult = try {
        if (!starter.canHandle(intent)) ArticleActionResult.Failed(ArticleActionResult.Reason.NoHandler)
        else { starter.start(intent); ArticleActionResult.Completed }
    } catch (_: RuntimeException) {
        ArticleActionResult.Failed(ArticleActionResult.Reason.LaunchFailed)
    }
}

data class ArticleDetailState(
    val article: Article?,
    val isOffline: Boolean,
    val isBookmarked: Boolean,
)