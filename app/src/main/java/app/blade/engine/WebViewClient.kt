package app.blade.engine

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

class WebViewClient(
    private val isAdBlockEnabled: () -> Boolean,
    private val onPageStarted: (url: String) -> Unit,
    private val onPageFinished: (url: String, view: WebView) -> Unit
) : WebViewClient() {

    private val adDomains = listOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adnxs.com",
        "advertising.com",
        "amazon-adsystem.com",
        "casalemedia.com",
        "openx.net",
        "pubmatic.com",
        "rubiconproject.com",
        "ads.youtube.com",
        "google-analytics.com",
        "scorecardresearch.com"
    )

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (isAdBlockEnabled()) {
            val url = request.url.toString()
            if (adDomains.any { url.contains(it) }) {
                return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream("".toByteArray()))
            }
        }
        return super.shouldInterceptRequest(view, request)
    }

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { onPageStarted(it) }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        url?.let { onPageFinished(it, view) }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return false
    }
}
