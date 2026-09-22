package com.example.liftlogic.data

import java.util.UUID

object BadgeKeys {
    const val STREAK_7 = "streak_7"
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
        val totalVolume = sets.fold(0f) { acc, s -> acc + (s.weightKg * s.reps) }

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

        val xpEarned = 10 + (totalVolume / 100).toInt()
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