package com.example.liftlogic.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey val workoutId: String,
    val userId: String,
    val date: Long,
    val durationSec: Int,
    val totalVolumeKg: Float,
    val synced: Boolean = false
)

@Entity(tableName = "exercise_sets")
data class ExerciseSetEntity(
    @PrimaryKey val setId: String,
    val workoutId: String,
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Float,
    val reps: Int,
    val setType: String = "normal"
)

@Entity(tableName = "body_metrics")
data class BodyMetricEntity(
    @PrimaryKey val metricId: String,
    val userId: String,
    val bodyWeightKg: Float,
    val recordedAt: Long
)

@Entity(tableName = "badges")
data class BadgeEntity(
    @PrimaryKey val badgeId: String,
    val userId: String,
    val badgeName: String,
    val unlockedAt: Long
)