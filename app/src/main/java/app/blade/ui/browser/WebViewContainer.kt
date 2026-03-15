package app.blade.ui.browser

import android.annotation.SuppressLint
import android.os.Build
import android.view.ViewGroup
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import app.blade.engine.WebChromeClient
import app.blade.engine.WebViewClient
import app.blade.engine.BrowserViewModel

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewContainer(
    url: String,
    viewModel: BrowserViewModel,
    onWebViewCreated: (WebView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                loadWithOverviewMode = true
                useWideViewPort = true
                safeBrowsingEnabled = true
            }
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            webViewClient = WebViewClient(
                onPageStarted = { u -> viewModel.onPageStarted(u) },
                onPageFinished = { u -> viewModel.onPageFinished(u, this) }
            )
            webChromeClient = WebChromeClient(
                onProgressChanged = { p -> viewModel.onProgressChanged(p) },
                onTitleReceived = { t -> viewModel.onTitleReceived(t) }
            )
        }
    }

    DisposableEffect(Unit) {
        onWebViewCreated(webView)
        webView.loadUrl(url)
        onDispose { webView.destroy() }
    }

    AndroidView(
        factory = { webView },
        update = { wv ->
            if (wv.url != url) {
                wv.loadUrl(url)
            }
        },
        modifier = modifier
    )
}
