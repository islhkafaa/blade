package app.blade.engine

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    fun onUrlInputSubmitted(input: String) {
        val normalized = normalizeInput(input)
        _state.update { it.copy(url = normalized) }
    }

    fun onPageStarted(url: String) {
        _state.update {
            it.copy(
                url = url,
                displayUrl = extractDisplayUrl(url),
                isLoading = true,
                progress = 0
            )
        }
    }

    fun onPageFinished(url: String, webView: WebView) {
        _state.update {
            it.copy(
                url = url,
                displayUrl = extractDisplayUrl(url),
                isLoading = false,
                progress = 100,
                canGoBack = webView.canGoBack(),
                canGoForward = webView.canGoForward()
            )
        }
    }

    fun onProgressChanged(progress: Int) {
        _state.update { it.copy(progress = progress) }
    }

    fun onTitleReceived(title: String) {
        _state.update { it.copy(title = title) }
    }

    fun goBack(webView: WebView) {
        if (webView.canGoBack()) {
            webView.goBack()
            _state.update {
                it.copy(
                    canGoBack = webView.canGoBack(),
                    canGoForward = webView.canGoForward()
                )
            }
        }
    }

    fun goForward(webView: WebView) {
        if (webView.canGoForward()) {
            webView.goForward()
            _state.update {
                it.copy(
                    canGoBack = webView.canGoBack(),
                    canGoForward = webView.canGoForward()
                )
            }
        }
    }

    fun reload(webView: WebView) {
        webView.reload()
    }

    private fun normalizeInput(input: String): String {
        val trimmed = input.trim()
        return when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> "https://www.google.com/search?q=${trimmed.replace(" ", "+")}"
        }
    }

    private fun extractDisplayUrl(url: String): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
    }
}
