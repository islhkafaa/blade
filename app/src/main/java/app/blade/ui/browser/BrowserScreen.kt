package app.blade.ui.browser

import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.blade.engine.BrowserViewModel

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                AddressBar(
                    displayUrl = state.displayUrl,
                    isLoading = state.isLoading,
                    onUrlSubmitted = { input -> viewModel.onUrlInputSubmitted(input) },
                    onReload = { webViewInstance?.let { viewModel.reload(it) } },
                    onStop = { webViewInstance?.stopLoading() }
                )

                if (state.isLoading) {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
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
                canGoBack = state.canGoBack,
                canGoForward = state.canGoForward,
                onBack = { webViewInstance?.let { viewModel.goBack(it) } },
                onForward = { webViewInstance?.let { viewModel.goForward(it) } }
            )
        }
    ) { innerPadding ->
        WebViewContainer(
            url = state.url,
            viewModel = viewModel,
            onWebViewCreated = { wv -> webViewInstance = wv },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
