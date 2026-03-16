package app.blade.di

import android.content.Context
import app.blade.engine.DownloadManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

import app.blade.data.DownloadDao

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideDownloadManager(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        downloadDao: DownloadDao
    ): DownloadManager {
        return DownloadManager(context, okHttpClient, downloadDao)
    }
}
