package app.blade

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import dagger.hilt.android.AndroidEntryPoint
import app.blade.ui.browser.BrowserScreen
import app.blade.ui.theme.BladeTheme
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.blade.engine.BrowserViewModel
import app.blade.data.SettingsRepository

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: BrowserViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val voiceSearchLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    val query = results[0]
                    viewModel.onUrlInputSubmitted(query)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settingsRepo.allSettings.collectAsState(initial = emptyList())
            val darkModePref =
                settings.find { it.key == SettingsRepository.KEY_DARK_MODE }?.value ?: "System"

            val isDark = when (darkModePref) {
                "Light" -> false
                "Dark" -> true
                else -> isSystemInDarkTheme()
            }

            BladeTheme(darkTheme = isDark) {
                BrowserScreen(
                    viewModel = viewModel,
                    onVoiceSearchClick = {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(
                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
                            )
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to search")
                        }
                        voiceSearchLauncher.launch(intent)
                    }
                )
            }
        }
    }
}
