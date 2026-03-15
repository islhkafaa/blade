package app.blade.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [HistoryEntity::class, BookmarkEntity::class], version = 1, exportSchema = true)
abstract class Database : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun bookmarkDao(): BookmarkDao
}
