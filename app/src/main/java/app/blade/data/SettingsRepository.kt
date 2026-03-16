package app.blade.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDao: SettingsDao
) {
    fun getSetting(key: String, defaultValue: String): Flow<String> {
        return settingsDao.getSetting(key).map { it?.value ?: defaultValue }
    }

    suspend fun saveSetting(key: String, value: String) {
        settingsDao.insertSetting(SettingsEntity(key, value))
    }

    val allSettings = settingsDao.getAllSettings()

    companion object {
        const val KEY_SEARCH_ENGINE = "search_engine"
        const val KEY_HOME_PAGE = "home_page"
        const val KEY_DARK_MODE = "dark_mode"
        const val KEY_AD_BLOCK = "ad_block"

        const val VAL_SEARCH_GOOGLE = "https://www.google.com/search?q="
        const val VAL_SEARCH_DUCKDUCKGO = "https://duckduckgo.com/?q="
        const val VAL_SEARCH_BING = "https://www.bing.com/search?q="
    }
}
