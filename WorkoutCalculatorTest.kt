package com.example.liftlogic.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [WorkoutCalculator], covering the total-volume and XP
 * formulas used when a workout is finished (see WorkoutRepository.finishWorkout).
 * These run on the local JVM (no device/emulator needed) since the
 * calculation logic has no Android or database dependency.
 */
class WorkoutCalculatorTest {

    @Test
    fun calculateTotalVolume_singleSet_returnsWeightTimesReps() {
        val sets = listOf(
            LoggedSetInput(exerciseId = "1", exerciseName = "Bench Press", weightKg = 60f, reps = 8)
        )

        val volume = WorkoutCalculator.calculateTotalVolume(sets)

        assertEquals(480f, volume, 0.001f)
    }

    @Test
    fun calculateTotalVolume_multipleSets_sumsEachSet() {
        val sets = listOf(
            LoggedSetInput(exerciseId = "1", exerciseName = "Bench Press", weightKg = 60f, reps = 8),
            LoggedSetInput(exerciseId = "1", exerciseName = "Bench Press", weightKg = 60f, reps = 8),
            LoggedSetInput(exerciseId = "2", exerciseName = "Squat", weightKg = 80f, reps = 5)
        )

        val volume = WorkoutCalculator.calculateTotalVolume(sets)

        // (60*8) + (60*8) + (80*5) = 480 + 480 + 400 = 1360
        assertEquals(1360f, volume, 0.001f)
    }

    @Test
    fun calculateTotalVolume_noSets_returnsZero() {
        val volume = WorkoutCalculator.calculateTotalVolume(emptyList())

        assertEquals(0f, volume, 0.001f)
    }

    @Test
    fun calculateXp_zeroVolume_returnsBaseXpOnly() {
        // Even an empty/very light workout should still earn the flat 10 XP
        // for logging a session, so users are never penalised to 0.
        val xp = WorkoutCalculator.calculateXp(0f)

        assertEquals(10, xp)
    }

    @Test
    fun calculateXp_addsOneXpPerHundredKgOfVolume() {
        val xp = WorkoutCalculator.calculateXp(480f)

        // base 10 + (480 / 100 = 4, rounded down) = 14
        assertEquals(14, xp)
    }

    @Test
    fun calculateXp_roundsDownPartialHundreds() {
        // 250kg is 2.5 "hundreds" — should round down to 2, not up to 3.
        val xp = WorkoutCalculator.calculateXp(250f)

        assertEquals(12, xp)
    }
}
