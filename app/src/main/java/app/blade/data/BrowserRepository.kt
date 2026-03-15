package app.blade.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserRepository @Inject constructor(
    private val historyDao: HistoryDao,
    private val bookmarkDao: BookmarkDao
) {
    val allHistory: Flow<List<HistoryEntity>> = historyDao.getAllHistory()
    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    suspend fun saveVisit(url: String, title: String) {
        if (url.isNotBlank() && !url.startsWith("chrome://") && !url.startsWith("about:")) {
            historyDao.insertHistory(HistoryEntity(url = url, title = title))
        }
    }

    suspend fun clearHistory() = historyDao.clearAllHistory()

    suspend fun deleteHistoryItem(history: HistoryEntity) = historyDao.deleteHistory(history)

    suspend fun isBookmarked(url: String): Boolean {
        return bookmarkDao.getBookmarkByUrl(url) != null
    }

    suspend fun toggleBookmark(url: String, title: String) {
        val existing = bookmarkDao.getBookmarkByUrl(url)
        if (existing != null) {
            bookmarkDao.deleteBookmark(existing)
        } else {
            bookmarkDao.insertBookmark(BookmarkEntity(url = url, title = title))
        }
    }
}
