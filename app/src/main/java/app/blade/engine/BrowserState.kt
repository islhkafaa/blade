package app.blade.engine

data class BrowserState(
    val url: String = "",
    val displayUrl: String = "",
    val title: String = "",
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isBookmarked: Boolean = false
)
