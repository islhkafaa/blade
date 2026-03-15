package app.blade.ui.browser

import android.os.Build
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.blade.data.ScreenType
import app.blade.engine.BrowserViewModel
import app.blade.ui.tabs.TabSwitcherScreen
import app.blade.ui.history.HistoryScreen
import app.blade.ui.bookmarks.BookmarksScreen
import app.blade.ui.settings.SettingsScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val isTabSwitcherVisible by viewModel.isTabSwitcherVisible.collectAsStateWithLifecycle()
    val isHistoryVisible by viewModel.isHistoryVisible.collectAsStateWithLifecycle()
    val isBookmarksVisible by viewModel.isBookmarksVisible.collectAsStateWithLifecycle()
    val isSettingsVisible by viewModel.isSettingsVisible.collectAsStateWithLifecycle()

    val historyItems by viewModel.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val bookmarkItems by viewModel.bookmarks.collectAsStateWithLifecycle(initialValue = emptyList())

    val activeTab = remember(tabs, activeTabId) {
        tabs.find { it.id == activeTabId } ?: tabs.first()
    }

    val webViews = remember { mutableStateMapOf<String, WebView>() }

    val currentScreen = when {
        isTabSwitcherVisible -> ScreenType.TabSwitcher
        isHistoryVisible -> ScreenType.History
        isBookmarksVisible -> ScreenType.Bookmarks
        isSettingsVisible -> ScreenType.Settings
        else -> ScreenType.Browser
    }

    BackHandler(enabled = currentScreen != ScreenType.Browser || activeTab.state.canGoBack) {
        when (currentScreen) {
            is ScreenType.TabSwitcher -> viewModel.toggleTabSwitcher()
            is ScreenType.History -> viewModel.toggleHistory()
            is ScreenType.Bookmarks -> viewModel.toggleBookmarks()
            is ScreenType.Settings -> viewModel.toggleSettings()
            is ScreenType.Browser -> webViews[activeTabId]?.goBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    AddressBar(
                        displayUrl = activeTab.state.displayUrl,
                        isBookmarked = activeTab.state.isBookmarked,
                        isLoading = activeTab.state.isLoading,
                        onUrlSubmitted = { input -> viewModel.onUrlInputSubmitted(input) },
                        onBookmarkToggle = { viewModel.toggleBookmark() },
                        onReload = { webViews[activeTabId]?.reload() },
                        onStop = { webViews[activeTabId]?.stopLoading() }
                    )

                    if (activeTab.state.isLoading) {
                        LinearProgressIndicator(
                            progress = { activeTab.state.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 12.dp)
                        )
                    }
                }
            },
            bottomBar = {
                BottomNavBar(
                    canGoBack = activeTab.state.canGoBack,
                    canGoForward = activeTab.state.canGoForward,
                    onBack = { webViews[activeTabId]?.let { viewModel.goBack(it) } },
                    onForward = { webViews[activeTabId]?.let { viewModel.goForward(it) } },
                    onTabsClick = { viewModel.toggleTabSwitcher() },
                    onHistoryClick = { viewModel.toggleHistory() },
                    onBookmarksClick = { viewModel.toggleBookmarks() },
                    onSettingsClick = { viewModel.toggleSettings() }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                tabs.forEach { tab ->
                    val isVisible = tab.id == activeTabId
                    Box(modifier = Modifier.fillMaxSize()) {
                        WebViewContainer(
                            url = tab.state.url,
                            viewModel = viewModel,
                            onWebViewCreated = { wv -> webViews[tab.id] = wv },
                            modifier = Modifier.fillMaxSize(),
                            isVisible = isVisible
                        )
                    }
                }
            }
        }

        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "overlay_transition"
        ) { screen ->
            when (screen) {
                is ScreenType.TabSwitcher -> {
                    TabSwitcherScreen(
                        tabs = tabs,
                        activeTabId = activeTabId,
                        onTabSelected = { viewModel.switchTab(it) },
                        onTabClosed = { viewModel.closeTab(it) },
                        onNewTab = { viewModel.createNewTab() },
                        onCloseSwitcher = { viewModel.toggleTabSwitcher() }
                    )
                }

                is ScreenType.History -> {
                    HistoryScreen(
                        historyItems = historyItems,
                        onItemClick = { url ->
                            viewModel.onUrlInputSubmitted(url)
                            viewModel.toggleHistory()
                        },
                        onDeleteItem = { viewModel.deleteHistoryItem(it) },
                        onClearAll = { viewModel.clearHistory() },
                        onBack = { viewModel.toggleHistory() }
                    )
                }

                is ScreenType.Bookmarks -> {
                    BookmarksScreen(
                        bookmarks = bookmarkItems,
                        onBookmarkClick = { url ->
                            viewModel.onUrlInputSubmitted(url)
                            viewModel.toggleBookmarks()
                        },
                        onDeleteBookmark = { viewModel.deleteBookmark(it) },
                        onBack = { viewModel.toggleBookmarks() }
                    )
                }

                is ScreenType.Settings -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.toggleSettings() }
                    )
                }

                is ScreenType.Browser -> {
                }
            }
        }
    }
}
