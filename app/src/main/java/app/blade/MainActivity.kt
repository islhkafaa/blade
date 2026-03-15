package app.blade

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import app.blade.ui.browser.BrowserScreen
import app.blade.ui.theme.BladeTheme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.blade.engine.BrowserViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import app.blade.data.SettingsRepository

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BrowserViewModel = hiltViewModel()
            val settings by viewModel.settingsRepo.allSettings.collectAsState(initial = emptyList())
            val darkModePref =
                settings.find { it.key == SettingsRepository.KEY_DARK_MODE }?.value ?: "System"

            val isDark = when (darkModePref) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            BladeTheme(darkTheme = isDark) {
                BrowserScreen(viewModel = viewModel)
            }
        }
    }
}
