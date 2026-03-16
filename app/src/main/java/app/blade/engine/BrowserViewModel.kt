package app.blade.engine

import android.webkit.WebView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.blade.data.BrowserRepository
import app.blade.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val repository: BrowserRepository,
    private val settingsRepository: SettingsRepository,
    private val downloadManager: DownloadManager
) : ViewModel() {

    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val searchSuggestions: StateFlow<List<SearchSuggestion>> = _searchSuggestions.asStateFlow()

    data class SearchSuggestion(
        val text: String,
        val type: SuggestionType
    )

    enum class SuggestionType {
        HISTORY, BOOKMARK, SEARCH
    }

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

    private val _isSettingsVisible = MutableStateFlow(false)
    val isSettingsVisible: StateFlow<Boolean> = _isSettingsVisible.asStateFlow()

    private val _isDownloadsVisible = MutableStateFlow(false)
    val isDownloadsVisible: StateFlow<Boolean> = _isDownloadsVisible.asStateFlow()

    val downloads: StateFlow<List<app.blade.data.DownloadEntity>> = downloadManager.downloads
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history = repository.allHistory
    val bookmarks = repository.allBookmarks
    val settingsRepo = settingsRepository

    private var currentSearchEngine = SettingsRepository.VAL_SEARCH_GOOGLE

    init {
        viewModelScope.launch {
            currentSearchEngine = settingsRepository.getSetting(
                SettingsRepository.KEY_SEARCH_ENGINE,
                SettingsRepository.VAL_SEARCH_GOOGLE
            ).first()
        }
    }

    fun createNewTab(url: String? = null) {
        viewModelScope.launch {
            val homepage = url
                ?: settingsRepository.getSetting(
                    SettingsRepository.KEY_HOME_PAGE,
                    "https://www.google.com"
                ).first()
            val newTab = TabInfo(
                state = BrowserState(
                    url = homepage,
                    displayUrl = extractDisplayUrl(homepage)
                )
            )
            _tabs.update { it + newTab }
            _activeTabId.value = newTab.id
            hideAllOverlays()
        }
    }

    private fun hideAllOverlays() {
        _isTabSwitcherVisible.value = false
        _isHistoryVisible.value = false
        _isBookmarksVisible.value = false
        _isSettingsVisible.value = false
        _isDownloadsVisible.value = false
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
        hideAllOverlays()
    }

    fun toggleTabSwitcher() {
        _isTabSwitcherVisible.update { !it }
        if (_isTabSwitcherVisible.value) {
            _isHistoryVisible.value = false
            _isBookmarksVisible.value = false
            _isSettingsVisible.value = false
        }
    }

    fun toggleHistory() {
        _isHistoryVisible.update { !it }
        if (_isHistoryVisible.value) {
            _isTabSwitcherVisible.value = false
            _isBookmarksVisible.value = false
            _isSettingsVisible.value = false
        }
    }

    fun toggleBookmarks() {
        _isBookmarksVisible.update { !it }
        if (_isBookmarksVisible.value) {
            _isTabSwitcherVisible.value = false
            _isHistoryVisible.value = false
            _isSettingsVisible.value = false
        }
    }

    fun toggleSettings() {
        _isSettingsVisible.update { !it }
        if (_isSettingsVisible.value) {
            _isTabSwitcherVisible.value = false
            _isHistoryVisible.value = false
            _isBookmarksVisible.value = false
            _isDownloadsVisible.value = false
        }
    }

    fun toggleDownloads() {
        _isDownloadsVisible.update { !it }
        if (_isDownloadsVisible.value) {
            _isTabSwitcherVisible.value = false
            _isHistoryVisible.value = false
            _isBookmarksVisible.value = false
            _isSettingsVisible.value = false
        }
    }

    fun deleteDownload(download: app.blade.data.DownloadEntity) {
        downloadManager.deleteDownload(download)
    }

    fun onSearchTextChanged(text: String) {
        if (text.isBlank()) {
            _searchSuggestions.value = emptyList()
            return
        }

        viewModelScope.launch {
            val localHistory = repository.searchHistory(text).first().take(3).map {
                SearchSuggestion(it.url, SuggestionType.HISTORY)
            }
            val localBookmarks = repository.searchBookmarks(text).first().take(3).map {
                SearchSuggestion(it.url, SuggestionType.BOOKMARK)
            }

            val apiSuggestions = fetchSearchSuggestions(text)

            _searchSuggestions.value =
                (localBookmarks + localHistory + apiSuggestions).distinctBy { it.text }
        }
    }

    private fun fetchSearchSuggestions(query: String): List<SearchSuggestion> {
        return try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url =
                URL("https://suggestqueries.google.com/complete/search?client=firefox&q=$encodedQuery")
            val connection = url.openConnection()
            val text = connection.getInputStream().bufferedReader().use { it.readText() }
            val json = JSONArray(text)
            val suggestionsJson = json.getJSONArray(1)
            val list = mutableListOf<SearchSuggestion>()
            for (i in 0 until suggestionsJson.length()) {
                list.add(SearchSuggestion(suggestionsJson.getString(i), SuggestionType.SEARCH))
            }
            list.take(5)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearDownloads() {
        downloadManager.clearAll()
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

    fun downloadFile(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?
    ) {
        downloadManager.downloadFile(url, userAgent, contentDisposition, mimeType)
    }

    fun updateSetting(key: String, value: String) {
        viewModelScope.launch {
            settingsRepository.saveSetting(key, value)
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
            else -> "$currentSearchEngine${trimmed.replace(" ", "+")}"
        }
    }

    private fun extractDisplayUrl(url: String): String {
        return url
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
    }
}
