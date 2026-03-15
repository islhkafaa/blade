package app.blade.engine

data class BrowserState(
    val url: String = "https://www.google.com",
    val displayUrl: String = "www.google.com",
    val title: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)
