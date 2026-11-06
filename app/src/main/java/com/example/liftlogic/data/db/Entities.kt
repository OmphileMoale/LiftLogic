package com.example.liftlogic.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/* ------------------------------------------------------------------
 * Room tables. All weights are stored in KILOGRAMS and converted for
 * display with util/Units.kt when the user prefers pounds.
 * ------------------------------------------------------------------ */

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey val userId: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val units: String = "kg",
    val xp: Int = 0,
    val notificationsEnabled: Boolean = true,
    val goal: String = "build_muscle",
    val experience: String = "beginner",
    val heightCm: Double? = null,
    val weeklyGoal: Int = 3,
    val defaultRestSec: Int = 90,
    val createdAt: Long = System.currentTimeMillis()
)

/** An exercise in the library. ownerUserId == null means it is a built-in exercise. */
@Entity(tableName = "exercises", indices = [Index("ownerUserId"), Index("name")])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val exerciseId: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val instructions: String = "",
    val isCustom: Boolean = false,
    val ownerUserId: String? = null,
    val remoteId: Int? = null
)

/** A workout template. ownerUserId == null means it is a built-in program. */
@Entity(tableName = "routines", indices = [Index("ownerUserId")])
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val routineId: Long = 0,
    val ownerUserId: String?,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "routine_exercises",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["routineId"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["exerciseId"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineId"), Index("exerciseId")]
)
data class RoutineExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int = 3,
    val targetReps: String = "8-12",
    val restSec: Int = 90
)

/** A logged workout. finishedAt == null means the workout is still in progress. */
@Entity(tableName = "workouts", indices = [Index("userId"), Index("finishedAt")])
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val workoutId: Long = 0,
    val userId: String,
    val name: String,
    val routineId: Long? = null,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val durationSec: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalSets: Int = 0,
    val xpEarned: Int = 0,
    val prCount: Int = 0,
    val notes: String = ""
)

@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutEntity::class,
            parentColumns = ["workoutId"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutId"), Index("exerciseId")]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val position: Int,
    val restSec: Int = 90,
    val targetReps: String = ""
)

@Entity(
    tableName = "workout_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseId"), Index("workoutId")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val setId: Long = 0,
    val workoutExerciseId: Long,
    val workoutId: Long,
    val setNumber: Int,
    val weightKg: Double? = null,
    val reps: Int? = null,
    val completed: Boolean = false,
    val isPr: Boolean = false
)

@Entity(tableName = "body_weights", indices = [Index("userId")])
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val weightKg: Double,
    val recordedAt: Long
)

@Entity(tableName = "badges", indices = [Index(value = ["userId", "badgeKey"], unique = true)])
data class BadgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val badgeKey: String,
    val unlockedAt: Long
)

/* ----------------------------- Query results ----------------------------- */

data class RoutineSummary(
    @Embedded val routine: RoutineEntity,
    val exerciseCount: Int
)

data class RoutineExerciseDetail(
    @Embedded val item: RoutineExerciseEntity,
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: String
)

/** One completed, weighted set from a finished workout (used for PRs, charts and "previous"). */
data class CompletedSet(
    val setId: Long,
    val workoutId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val finishedAt: Long
)
