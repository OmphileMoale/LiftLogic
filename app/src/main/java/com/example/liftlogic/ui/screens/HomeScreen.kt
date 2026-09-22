package com.example.liftlogic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.liftlogic.R
import com.example.liftlogic.ui.AppViewModel
import com.example.liftlogic.ui.theme.LiftGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onStartWorkout: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val history by viewModel.workoutHistory().collectAsState(initial = emptyList())

    Scaffold(
        bottomBar = {
            LiftLogicBottomBar(
                current = BottomTab.HOME,
                onHome = {}, onWorkouts = onOpenLibrary, onProgress = onOpenProgress, onProfile = onOpenProfile
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                GreetingSection(user?.name ?: "Athlete")
            }

            item {
                StatsDashboard(
                    streak = user?.streakDays ?: 0,
                    level = 1 + (user?.xp ?: 0) / 200,
                    xp = user?.xp ?: 0
                )
            }

            item {
                SuggestedWorkoutCard(onStartWorkout)
            }

            item {
                QuickActionsSection(onStartWorkout, onOpenLibrary, onOpenProgress)
            }

            item {
                SectionHeader(stringResource(R.string.title_recent_workouts))
            }

            if (history.isEmpty()) {
                item {
                    EmptyHistoryPlaceholder()
                }
            } else {
                items(history.take(5)) { w ->
                    RecentWorkoutItem(w.date, w.totalVolumeKg)
                }
            }
            
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun GreetingSection(name: String) {
    val calendar = Calendar.getInstance()
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 0..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Column {
        Text(
            text = "$greeting,",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun StatsDashboard(streak: Int, level: Int, xp: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            label = "Streak",
            value = "$streak",
            unit = "Days",
            icon = Icons.Default.Whatshot,
            color = Color(0xFFFF9800),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Level",
            value = "$level",
            unit = "Rank",
            icon = Icons.Default.EmojiEvents,
            color = Color(0xFFFFD700),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "XP",
            value = "$xp",
            unit = "Points",
            icon = Icons.Default.AutoAwesome,
            color = LiftGreen,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(label: String, value: String, unit: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SuggestedWorkoutCard(onStart: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                )
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.title_suggested_workout),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.sample_workout_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.sample_workout_detail),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_start))
                }
            }
        }
    }
}

@Composable
fun QuickActionsSection(onStart: () -> Unit, onLibrary: () -> Unit, onProgress: () -> Unit) {
    Column {
        SectionHeader(stringResource(R.string.title_quick_actions))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "New Workout",
                onClick = onStart,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.MenuBook,
                label = "Library",
                onClick = onLibrary,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.BarChart,
                label = "Stats",
                onClick = onProgress,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun QuickActionButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.height(4.dp))
            Text(label, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
fun RecentWorkoutItem(date: Long, volume: Float) {
    val dateStr = SimpleDateFormat("EEE, d MMM HH:mm", Locale.getDefault()).format(Date(date))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.format_workout_dated, dateStr), style = MaterialTheme.typography.titleSmall)
                Text(
                    "${volume.toInt()} kg total volume",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun EmptyHistoryPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_recent_workouts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
    }
}
