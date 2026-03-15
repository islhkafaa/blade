package app.blade.engine

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.blade.data.BrowserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val repository: BrowserRepository
) : ViewModel() {

    private val _tabs = MutableStateFlow(listOf(TabInfo()))
    val tabs: StateFlow<List<TabInfo>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow(_tabs.value.first().id)
    val activeTabId: StateFlow<String> = _activeTabId.asStateFlow()

    private val _isTabSwitcherVisible = MutableStateFlow(false)
    val isTabSwitcherVisible: StateFlow<Boolean> = _isTabSwitcherVisible.asStateFlow()

    private val _isHistoryVisible = MutableStateFlow(false)
    val isHistoryVisible: StateFlow<Boolean> = _isHistoryVisible.asStateFlow()

    private val _isBookmarksVisible = MutableStateFlow(false)
    val isBookmarksVisible: StateFlow<Boolean> = _isBookmarksVisible.asStateFlow()

    val history = repository.allHistory
    val bookmarks = repository.allBookmarks

    fun createNewTab(url: String? = "https://www.google.com") {
        val newTab = TabInfo(state = BrowserState(url = url ?: "https://www.google.com"))
        _tabs.update { it + newTab }
        _activeTabId.value = newTab.id
        _isTabSwitcherVisible.value = false
        _isHistoryVisible.value = false
        _isBookmarksVisible.value = false
    }

    fun closeTab(tabId: String) {
        _tabs.update { tabs ->
            val updated = tabs.filter { it.id != tabId }
            updated.ifEmpty { listOf(TabInfo()) }
        }
        if (_activeTabId.value == tabId) {
            _activeTabId.value = _tabs.value.last().id
        }
    }

    fun switchTab(tabId: String) {
        _activeTabId.value = tabId
        _isTabSwitcherVisible.value = false
        _isHistoryVisible.value = false
        _isBookmarksVisible.value = false
    }

    fun toggleTabSwitcher() {
        _isTabSwitcherVisible.update { !it }
        if (_isTabSwitcherVisible.value) {
            _isHistoryVisible.value = false
            _isBookmarksVisible.value = false
        }
    }

    fun toggleHistory() {
        _isHistoryVisible.update { !it }
        if (_isHistoryVisible.value) {
            _isTabSwitcherVisible.value = false
            _isBookmarksVisible.value = false
        }
    }

    fun toggleBookmarks() {
        _isBookmarksVisible.update { !it }
        if (_isBookmarksVisible.value) {
            _isTabSwitcherVisible.value = false
            _isHistoryVisible.value = false
        }
    }

    fun onUrlInputSubmitted(input: String) {
        val normalized = normalizeInput(input)
        updateActiveTab { it.copy(url = normalized) }
    }

    fun onPageStarted(url: String) {
        updateActiveTab {
            it.copy(
                url = url,
                displayUrl = extractDisplayUrl(url),
                isLoading = true,
                progress = 0
            )
        }
    }

    fun onPageFinished(url: String, webView: WebView) {
        val title = webView.title ?: ""
        updateActiveTab {
            it.copy(
                url = url,
                displayUrl = extractDisplayUrl(url),
                title = title,
                isLoading = false,
                progress = 100,
                canGoBack = webView.canGoBack(),
                canGoForward = webView.canGoForward()
            )
        }
        viewModelScope.launch {
            repository.saveVisit(url, title)
            val bookmarked = repository.isBookmarked(url)
            updateActiveTab { it.copy(isBookmarked = bookmarked) }
        }
    }

    fun onProgressChanged(progress: Int) {
        updateActiveTab { it.copy(progress = progress) }
    }

    fun onTitleReceived(title: String) {
        updateActiveTab { it.copy(title = title) }
    }

    fun toggleBookmark() {
        val activeTab = tabs.value.find { it.id == activeTabId.value }?.state ?: return
        viewModelScope.launch {
            repository.toggleBookmark(activeTab.url, activeTab.title)
            val isBookmarked = repository.isBookmarked(activeTab.url)
            updateActiveTab { it.copy(isBookmarked = isBookmarked) }
        }
    }

    fun deleteHistoryItem(item: app.blade.data.HistoryEntity) {
        viewModelScope.launch { repository.deleteHistoryItem(item) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory() }
    }

    fun deleteBookmark(item: app.blade.data.BookmarkEntity) {
        viewModelScope.launch { repository.toggleBookmark(item.url, item.title) }
    }

    fun goBack(webView: WebView) {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    fun goForward(webView: WebView) {
        if (webView.canGoForward()) {
            webView.goForward()
        }
    }

    private fun updateActiveTab(transform: (BrowserState) -> BrowserState) {
        val currentId = _activeTabId.value
        _tabs.update { tabs ->
            tabs.map { tab ->
                if (tab.id == currentId) tab.copy(state = transform(tab.state)) else tab
            }
        }
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
