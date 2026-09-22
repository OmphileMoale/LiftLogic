package com.example.liftlogic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liftlogic.R
import com.example.liftlogic.data.BadgeKeys
import com.example.liftlogic.ui.AppViewModel

@Composable
private fun badgeLabel(key: String): String = when (key) {
    BadgeKeys.STREAK_7 -> stringResource(R.string.badge_streak_7)
    else -> key
}

@Composable
fun WorkoutSummaryScreen(
    viewModel: AppViewModel,
    onBackToHome: () -> Unit
) {
    val result by viewModel.lastResult.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.title_workout_summary), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.subtitle_great_session), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(20.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip(
                stringResource(R.string.format_kg, result?.totalVolumeKg?.toInt() ?: 0),
                stringResource(R.string.label_total_volume),
                Modifier.weight(1f)
            )
            StatChip(
                stringResource(R.string.format_xp_earned, result?.xpEarned ?: 0),
                stringResource(R.string.label_xp_earned),
                Modifier.weight(1f)
            )
        }

        val badges = result?.newBadges.orEmpty()
        if (badges.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.title_badges_unlocked), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.forEach { key -> AssistChip(onClick = {}, label = { Text(badgeLabel(key)) }) }
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(onClick = onBackToHome, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.action_back_to_home)) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { /* share */ }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_share_summary))
        }
    }
}