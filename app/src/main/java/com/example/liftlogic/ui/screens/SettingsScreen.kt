package com.example.liftlogic.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.example.liftlogic.ui.AppViewModel
import com.example.liftlogic.ui.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onLoggedOut: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item { SettingsHeader("User Settings") }
            item { SettingsItem(Icons.Default.Person, "Edit Profile") { onNavigate(Routes.EDIT_PROFILE) } }
            item { SettingsItem(Icons.Default.Email, "Email / Username") { onNavigate(Routes.EMAIL_USERNAME) } }
            item { SettingsItem(Icons.Default.Lock, "Change Password") { onNavigate(Routes.CHANGE_PASSWORD) } }
            item { SettingsItem(Icons.Default.Image, "Profile Picture") { onNavigate(Routes.PROFILE_PICTURE) } }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            item { SettingsHeader("App Appearance") }
            item { DarkModeToggle() }
            item { SettingsItem(Icons.Default.Palette, "Theme") { onNavigate(Routes.THEME) } }
            item { LanguageSelector() }

            item { HorizontalDivider(Modifier.padding(vertical = 8.dp)) }

            item {
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = "Log Out",
                    textColor = MaterialTheme.colorScheme.error,
                    onClick = {
                        viewModel.logout()
                        onLoggedOut()
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(title, color = textColor) },
        leadingContent = { 
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (textColor == MaterialTheme.colorScheme.error) textColor else MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        modifier = Modifier.clickable { onClick() }
    )
}

@Composable
fun DarkModeToggle() {
    var isDark by remember { mutableStateOf(true) }
    ListItem(
        headlineContent = { Text("Dark Mode") },
        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
        trailingContent = {
            Switch(checked = isDark, onCheckedChange = { isDark = it })
        }
    )
}

@Composable
fun LanguageSelector() {
    var showDialog by remember { mutableStateOf(false) }
    val currentLanguage = AppCompatDelegate.getApplicationLocales()[0]?.language ?: "en"

    val languages = listOf(
        Triple("English", "en", "English"),
        Triple("Afrikaans", "af", "Afrikaans"),
        Triple("isiZulu", "zu", "Zulu")
    )

    ListItem(
        headlineContent = { Text("Language") },
        supportingContent = { 
            val label = languages.find { it.second == currentLanguage }?.first ?: "English"
            Text(label) 
        },
        leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
        modifier = Modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    languages.forEach { (name, code, _) ->
                        LanguageOption(name, currentLanguage == code) {
                            setLanguage(code)
                            showDialog = false
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LanguageOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun setLanguage(languageCode: String) {
    val appLocales: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocales)
}
