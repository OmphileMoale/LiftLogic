package com.example.liftlogic.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liftlogic.R
import com.example.liftlogic.ui.AppViewModel
import com.example.liftlogic.ui.theme.LiftGreen

@Composable
fun ProgressScreen(
    viewModel: AppViewModel,
    onHome: () -> Unit,
    onLibrary: () -> Unit,
    onProfile: () -> Unit
) {
    val history by viewModel.workoutHistory().collectAsState(initial = emptyList())
    val volumes = remember(history) { history.sortedBy { it.date }.map { it.totalVolumeKg } }

    Scaffold(
        bottomBar = {
            LiftLogicBottomBar(
                current = BottomTab.PROGRESS,
                onHome = onHome, onWorkouts = onLibrary, onProgress = {}, onProfile = onProfile
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.tab_progress), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            Text(stringResource(R.string.title_training_volume), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Card(Modifier.fillMaxWidth().height(180.dp)) {
                if (volumes.size < 2) {
                    Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text(stringResource(R.string.empty_progress_chart))
                    }
                } else {
                    VolumeLineChart(volumes, Modifier.fillMaxSize().padding(12.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.format_total_workouts, history.size), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.text_progress_footnote),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun VolumeLineChart(values: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)
        val points = values.mapIndexed { i, v ->
            Offset(x = i * stepX, y = size.height - (v / max) * size.height)
        }
        for (i in 0 until points.size - 1) {
            drawLine(color = LiftGreen, start = points[i], end = points[i + 1], strokeWidth = 6f)
        }
        points.forEach { drawCircle(color = Color.White, radius = 6f, center = it) }
    }
}