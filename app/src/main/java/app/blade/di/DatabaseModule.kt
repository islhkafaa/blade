package app.blade.di

import android.content.Context
import androidx.room.Room
import app.blade.data.Database
import app.blade.data.BookmarkDao
import app.blade.data.HistoryDao
import app.blade.data.SettingsDao
import app.blade.data.DownloadDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideBladeDatabase(@ApplicationContext context: Context): Database {
        return Room.databaseBuilder(
            context,
            Database::class.java,
            "blade_database"
        ).fallbackToDestructiveMigration(false).build()
    }

    @Provides
    fun provideHistoryDao(database: Database): HistoryDao {
        return database.historyDao()
    }

    @Provides
    fun provideBookmarkDao(database: Database): BookmarkDao {
        return database.bookmarkDao()
    }

    @Provides
    fun provideSettingsDao(database: Database): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    fun provideDownloadDao(database: Database): DownloadDao {
        return database.downloadDao()
    }
}
