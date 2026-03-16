package app.blade.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [HistoryEntity::class, BookmarkEntity::class, SettingsEntity::class, DownloadEntity::class],
    version = 2,
    exportSchema = true
)
abstract class Database : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun settingsDao(): SettingsDao
    abstract fun downloadDao(): DownloadDao
}
