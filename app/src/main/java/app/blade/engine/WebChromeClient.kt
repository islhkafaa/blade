package app.blade.engine

import android.webkit.WebChromeClient
import android.webkit.WebView

class WebChromeClient(
    private val onProgressChanged: (progress: Int) -> Unit,
    private val onTitleReceived: (title: String) -> Unit
) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged(newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        super.onReceivedTitle(view, title)
        title?.let { onTitleReceived(it) }
    }
}
