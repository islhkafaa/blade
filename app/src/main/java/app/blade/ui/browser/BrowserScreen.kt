package app.blade.ui.browser

import android.os.Build
import android.webkit.WebView
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
import app.blade.engine.BrowserViewModel
import app.blade.ui.tabs.TabSwitcherScreen

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val tabs by viewModel.tabs.collectAsStateWithLifecycle()
    val activeTabId by viewModel.activeTabId.collectAsStateWithLifecycle()
    val isTabSwitcherVisible by viewModel.isTabSwitcherVisible.collectAsStateWithLifecycle()

    val activeTab = remember(tabs, activeTabId) {
        tabs.find { it.id == activeTabId } ?: tabs.first()
    }

    val webViews = remember { mutableStateMapOf<String, WebView>() }

    AnimatedContent(
        targetState = isTabSwitcherVisible,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "browser_tab_switcher_transition"
    ) { showTabs ->
        if (showTabs) {
            TabSwitcherScreen(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabSelected = { viewModel.switchTab(it) },
                onTabClosed = { viewModel.closeTab(it) },
                onNewTab = { viewModel.createNewTab() },
                onCloseSwitcher = { viewModel.toggleTabSwitcher() }
            )
        } else {
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
                            isLoading = activeTab.state.isLoading,
                            onUrlSubmitted = { input -> viewModel.onUrlInputSubmitted(input) },
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
                        onTabsClick = { viewModel.toggleTabSwitcher() }
                    )
                }
            ) { innerPadding ->
                WebViewContainer(
                    url = activeTab.state.url,
                    viewModel = viewModel,
                    onWebViewCreated = { wv -> webViews[activeTabId] = wv },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}
