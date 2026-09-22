package com.example.liftlogic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liftlogic.R
import com.example.liftlogic.ui.AppViewModel

@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onProgress: () -> Unit,
    onSettings: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    var units by remember(user) { mutableStateOf(user?.units ?: "kg") }
    var notificationsEnabled by remember(user) { mutableStateOf(user?.notificationsEnabled ?: true) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            LiftLogicBottomBar(
                current = BottomTab.PROFILE,
                onHome = onHome, onWorkouts = onLibrary, onProgress = onProgress, onProfile = {}
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.tab_profile), style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(user?.name ?: "", style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.format_level_xp, 1 + (user?.xp ?: 0) / 200, user?.xp ?: 0),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.title_preferred_units), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = units == "kg",
                    onClick = { units = "kg"; saved = false },
                    label = { Text(stringResource(R.string.unit_kg)) }
                )
                FilterChip(
                    selected = units == "lb",
                    onClick = { units = "lb"; saved = false },
                    label = { Text(stringResource(R.string.unit_lb)) }
                )
            }

            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.label_notifications), style = MaterialTheme.typography.titleMedium)
                Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it; saved = false })
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.updateSettings(units, notificationsEnabled); saved = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.action_save_settings)) }
            if (saved) {
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.text_settings_saved), color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.weight(1f))
            OutlinedButton(
                onClick = { viewModel.logout(); onLoggedOut() },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.action_log_out)) }
        }
    }
}