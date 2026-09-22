package com.example.liftlogic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.liftlogic.R
import com.example.liftlogic.ui.AppViewModel

@Composable
private fun categories(): List<Pair<String, Int?>> = listOf(
    stringResource(R.string.category_all) to null,
    stringResource(R.string.category_chest) to 11,
    stringResource(R.string.category_back) to 12,
    stringResource(R.string.category_legs) to 9,
    stringResource(R.string.category_shoulders) to 13,
    stringResource(R.string.category_arms) to 8
)

@Composable
fun ExerciseLibraryScreen(
    viewModel: AppViewModel,
    onExercisePicked: (String) -> Unit,
    onSkipToWorkout: () -> Unit,
    onHome: () -> Unit,
    onProgress: () -> Unit,
    onProfile: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    val exercises by viewModel.exercises.collectAsState()
    val loading by viewModel.libraryLoading.collectAsState()
    val error by viewModel.libraryError.collectAsState()

    LaunchedEffect(selectedCategory) { viewModel.loadExercises(selectedCategory) }

    Scaffold(
        bottomBar = {
            LiftLogicBottomBar(
                current = BottomTab.WORKOUTS,
                onHome = onHome, onWorkouts = {}, onProgress = onProgress, onProfile = onProfile
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(stringResource(R.string.title_exercise_library), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories().forEach { (label, id) ->
                    FilterChip(
                        selected = selectedCategory == id,
                        onClick = { selectedCategory = id },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            Button(onClick = onSkipToWorkout, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.action_start_empty_workout))
            }
            Spacer(Modifier.height(12.dp))

            val unnamedExercise = stringResource(R.string.fallback_unnamed_exercise)
            val tapToAdd = stringResource(R.string.fallback_tap_to_add)
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(exercises) { ex ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onExercisePicked(ex.id.toString()) }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(ex.name ?: unnamedExercise, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    ex.cleanDescription().take(90).ifBlank { tapToAdd },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}