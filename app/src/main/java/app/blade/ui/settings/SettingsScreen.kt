package app.blade.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.blade.data.SettingsRepository
import app.blade.engine.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settingsRepo.allSettings.collectAsState(initial = emptyList())
    var showSearchDialog by remember { mutableStateOf(false) }
    var showHomeDialog by remember { mutableStateOf(false) }

    val searchEngineUrl = settings.find { it.key == SettingsRepository.KEY_SEARCH_ENGINE }?.value
        ?: SettingsRepository.VAL_SEARCH_GOOGLE

    val homePage = settings.find { it.key == SettingsRepository.KEY_HOME_PAGE }?.value
        ?: "https://www.google.com"

    val darkMode = settings.find { it.key == SettingsRepository.KEY_DARK_MODE }?.value
        ?: "System"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            item {
                PreferenceCategory(title = "General")
            }
            item {
                PreferenceItem(
                    title = "Search Engine",
                    summary = when (searchEngineUrl) {
                        SettingsRepository.VAL_SEARCH_GOOGLE -> "Google"
                        SettingsRepository.VAL_SEARCH_DUCKDUCKGO -> "DuckDuckGo"
                        SettingsRepository.VAL_SEARCH_BING -> "Bing"
                        else -> "Custom"
                    },
                    onClick = { showSearchDialog = true }
                )
            }
            item {
                PreferenceItem(
                    title = "Homepage",
                    summary = homePage,
                    onClick = { showHomeDialog = true }
                )
            }
            item {
                var showDarkDialog by remember { mutableStateOf(false) }

                PreferenceItem(
                    title = "Dark Mode",
                    summary = darkMode,
                    onClick = { showDarkDialog = true }
                )

                if (showDarkDialog) {
                    AlertDialog(
                        onDismissRequest = { showDarkDialog = false },
                        title = { Text("Dark Mode") },
                        text = {
                            Column {
                                SelectionOption("System Default", "System", darkMode) {
                                    viewModel.updateSetting(SettingsRepository.KEY_DARK_MODE, it)
                                    showDarkDialog = false
                                }
                                SelectionOption("Light", "Light", darkMode) {
                                    viewModel.updateSetting(SettingsRepository.KEY_DARK_MODE, it)
                                    showDarkDialog = false
                                }
                                SelectionOption("Dark", "Dark", darkMode) {
                                    viewModel.updateSetting(SettingsRepository.KEY_DARK_MODE, it)
                                    showDarkDialog = false
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDarkDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
            item {
                PreferenceCategory(title = "Privacy")
            }
            item {
                val isAdBlockEnabled =
                    settings.find { it.key == SettingsRepository.KEY_AD_BLOCK }?.value == "true"
                PreferenceSwitch(
                    title = "Ad-Blocker",
                    summary = "Block known intrusive advertisements",
                    checked = isAdBlockEnabled,
                    onCheckedChange = {
                        viewModel.updateSetting(SettingsRepository.KEY_AD_BLOCK, it.toString())
                    }
                )
            }
            item {
                var showClearDialog by remember { mutableStateOf(false) }

                PreferenceItem(
                    title = "Clear All Data",
                    summary = "Delete history, bookmarks, and downloads",
                    onClick = { showClearDialog = true }
                )

                if (showClearDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearDialog = false },
                        title = { Text("Clear Data") },
                        text = { Text("This will permanently delete your browsing history, bookmarks, and download list. This action cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.clearAllData()
                                    showClearDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Clear All") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search Engine") },
            text = {
                Column {
                    SelectionOption(
                        "Google",
                        SettingsRepository.VAL_SEARCH_GOOGLE,
                        searchEngineUrl
                    ) {
                        viewModel.updateSetting(SettingsRepository.KEY_SEARCH_ENGINE, it)
                        showSearchDialog = false
                    }
                    SelectionOption(
                        "DuckDuckGo",
                        SettingsRepository.VAL_SEARCH_DUCKDUCKGO,
                        searchEngineUrl
                    ) {
                        viewModel.updateSetting(SettingsRepository.KEY_SEARCH_ENGINE, it)
                        showSearchDialog = false
                    }
                    SelectionOption("Bing", SettingsRepository.VAL_SEARCH_BING, searchEngineUrl) {
                        viewModel.updateSetting(SettingsRepository.KEY_SEARCH_ENGINE, it)
                        showSearchDialog = false
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showHomeDialog) {
        var text by remember { mutableStateOf(homePage) }
        AlertDialog(
            onDismissRequest = { showHomeDialog = false },
            title = { Text("Homepage") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSetting(SettingsRepository.KEY_HOME_PAGE, text)
                    showHomeDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showHomeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SelectionOption(name: String, value: String, currentValue: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(value) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = value == currentValue, onClick = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(name)
    }
}

@Composable
fun PreferenceCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
fun PreferenceItem(
    title: String,
    summary: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PreferenceSwitch(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
