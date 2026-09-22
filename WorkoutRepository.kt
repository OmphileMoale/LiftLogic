package com.example.liftlogic.data

import java.util.UUID

object BadgeKeys {
    const val STREAK_7 = "streak_7"
}

/**
 * Pure calculation logic for finishing a workout, kept separate from the DAO
 * calls in [WorkoutRepository] so it can be unit tested directly (see
 * app/src/test/.../WorkoutCalculatorTest.kt) without needing a database or an
 * Android runtime.
 */
object WorkoutCalculator {

    /** Total volume lifted = sum of (weight x reps) across every logged set. */
    fun calculateTotalVolume(sets: List<LoggedSetInput>): Float =
        sets.fold(0f) { acc, s -> acc + (s.weightKg * s.reps) }

    /**
     * XP earned for a completed workout: a flat 10 XP for showing up, plus 1 XP
     * per 100kg of total volume lifted (rounded down), rewarding both
     * consistency (NFR1: fast logging encourages regular use) and effort.
     */
    fun calculateXp(totalVolumeKg: Float): Int =
        10 + (totalVolumeKg / 100).toInt()
}

data class LoggedSetInput(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Float,
    val reps: Int,
    val setType: String = "normal"
)

data class FinishWorkoutResult(
    val workoutId: String,
    val totalVolumeKg: Float,
    val xpEarned: Int,
    val newBadges: List<String>
)

class WorkoutRepository(
    private val workoutDao: WorkoutDao,
    private val userDao: UserDao,
    private val badgeDao: BadgeDao
) {

    suspend fun finishWorkout(
        userId: String,
        durationSec: Int,
        sets: List<LoggedSetInput>
    ): FinishWorkoutResult {
        val workoutId = UUID.randomUUID().toString()
        val totalVolume = WorkoutCalculator.calculateTotalVolume(sets)

        workoutDao.insertWorkout(
            WorkoutEntity(
                workoutId = workoutId,
                userId = userId,
                date = System.currentTimeMillis(),
                durationSec = durationSec,
                totalVolumeKg = totalVolume,
                synced = false
            )
        )
        workoutDao.insertSets(
            sets.map {
                ExerciseSetEntity(
                    setId = UUID.randomUUID().toString(),
                    workoutId = workoutId,
                    exerciseId = it.exerciseId,
                    exerciseName = it.exerciseName,
                    weightKg = it.weightKg,
                    reps = it.reps,
                    setType = it.setType
                )
            }
        )

        val xpEarned = WorkoutCalculator.calculateXp(totalVolume)
        val user = userDao.getUser(userId)
        val newBadges = mutableListOf<String>()

        if (user != null) {
            val newStreak = user.streakDays + 1
            val newXp = user.xp + xpEarned
            userDao.update(user.copy(xp = newXp, streakDays = newStreak))

            if (newStreak == 7) {
                newBadges += BadgeKeys.STREAK_7
                badgeDao.insert(BadgeEntity(UUID.randomUUID().toString(), userId, BadgeKeys.STREAK_7, System.currentTimeMillis()))
            }
        }

        return FinishWorkoutResult(workoutId, totalVolume, xpEarned, newBadges)
    }
}