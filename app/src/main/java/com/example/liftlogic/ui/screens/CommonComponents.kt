package com.example.liftlogic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liftlogic.R
import com.example.liftlogic.data.AuthError

enum class BottomTab { HOME, WORKOUTS, PROGRESS, PROFILE }

@Composable
fun AuthError.asMessage(): String = when (this) {
    AuthError.EMPTY_FIELDS -> stringResource(R.string.error_empty_fields)
    AuthError.EMAIL_TAKEN -> stringResource(R.string.error_email_taken)
    AuthError.NO_ACCOUNT -> stringResource(R.string.error_no_account)
    AuthError.WRONG_PASSWORD -> stringResource(R.string.error_wrong_password)
}

@Composable
fun LiftLogicBottomBar(
    current: BottomTab,
    onHome: () -> Unit,
    onWorkouts: () -> Unit,
    onProgress: () -> Unit,
    onProfile: () -> Unit
) {
    val homeLabel = stringResource(R.string.tab_home)
    val workoutsLabel = stringResource(R.string.tab_workouts)
    val progressLabel = stringResource(R.string.tab_progress)
    val profileLabel = stringResource(R.string.tab_profile)

    NavigationBar {
        NavigationBarItem(
            selected = current == BottomTab.HOME,
            onClick = onHome,
            icon = { Icon(Icons.Filled.Home, contentDescription = homeLabel) },
            label = { Text(homeLabel) }
        )
        NavigationBarItem(
            selected = current == BottomTab.WORKOUTS,
            onClick = onWorkouts,
            icon = { Icon(Icons.Filled.FitnessCenter, contentDescription = workoutsLabel) },
            label = { Text(workoutsLabel) }
        )
        NavigationBarItem(
            selected = current == BottomTab.PROGRESS,
            onClick = onProgress,
            icon = { Icon(Icons.Filled.ShowChart, contentDescription = progressLabel) },
            label = { Text(progressLabel) }
        )
        NavigationBarItem(
            selected = current == BottomTab.PROFILE,
            onClick = onProfile,
            icon = { Icon(Icons.Filled.Person, contentDescription = profileLabel) },
            label = { Text(profileLabel) }
        )
    }
}

@Composable
fun StatChip(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}