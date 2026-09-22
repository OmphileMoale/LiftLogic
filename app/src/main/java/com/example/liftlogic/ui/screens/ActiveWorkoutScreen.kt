package com.example.liftlogic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.liftlogic.R
import com.example.liftlogic.ui.AppViewModel
import kotlinx.coroutines.delay

@Composable
fun ActiveWorkoutScreen(
    viewModel: AppViewModel,
    onFinish: () -> Unit
) {
    val sets by viewModel.activeSets.collectAsState()

    val defaultExerciseName = stringResource(R.string.default_exercise_name)
    var exerciseName by remember { mutableStateOf(defaultExerciseName) }
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }

    var elapsedSec by remember { mutableStateOf(0) }
    var restRemaining by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) { delay(1000); elapsedSec++; if (restRemaining > 0) restRemaining-- }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            stringResource(R.string.format_active_workout_title, "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)),
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = exerciseName, onValueChange = { exerciseName = it },
            label = { Text(stringResource(R.string.label_exercise)) }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = weight, onValueChange = { weight = it },
                label = { Text(stringResource(R.string.label_kg)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = reps, onValueChange = { reps = it },
                label = { Text(stringResource(R.string.label_reps)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val w = weight.toFloatOrNull()
                val r = reps.toIntOrNull()
                if (w != null && r != null && exerciseName.isNotBlank()) {
                    viewModel.addSet(exerciseId = exerciseName.lowercase(), exerciseName = exerciseName, weightKg = w, reps = r)
                    restRemaining = 90
                    weight = ""; reps = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.action_add_set)) }

        if (restRemaining > 0) {
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.format_rest_timer, "%02d:%02d".format(restRemaining / 60, restRemaining % 60))) }
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.title_logged_sets), style = MaterialTheme.typography.titleMedium)
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(sets) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(s.exerciseName)
                        Text(stringResource(R.string.format_set_row, s.weightKg.toString(), s.reps))
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { viewModel.finishWorkout(elapsedSec, onFinish) },
            enabled = sets.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.action_finish_workout)) }
    }
}