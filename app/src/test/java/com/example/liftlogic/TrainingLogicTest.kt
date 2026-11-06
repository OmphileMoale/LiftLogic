package com.example.liftlogic

import com.example.liftlogic.data.db.CompletedSet
import com.example.liftlogic.data.db.WorkoutEntity
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.OneRepMax
import com.example.liftlogic.util.PlateCalculator
import com.example.liftlogic.util.Stats
import com.example.liftlogic.util.Units
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Unit tests for the pure training logic (run with ./gradlew test). */
class TrainingLogicTest {

    private fun millis(d: LocalDate, hour: Int = 12) =
        d.atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private val today = LocalDate.of(2026, 11, 4) // a Wednesday

    @Test
    fun unitConversion_roundTrips() {
        assertEquals(220.46, Units.fromKg(100.0, Units.LB), 0.01)
        assertEquals("135", Units.number(Units.display(Units.toKg(135.0, Units.LB), Units.LB)))
        assertEquals(62.5, Units.parse("62,5")!!, 0.0)
        assertNull(Units.parse(" "))
    }

    @Test
    fun oneRepMax_epley() {
        assertEquals(100.0, OneRepMax.estimate(100.0, 1), 0.0)
        assertEquals(133.33, OneRepMax.estimate(100.0, 10), 0.01)
        assertEquals(0.0, OneRepMax.estimate(100.0, 0), 0.0)
    }

    @Test
    fun plateCalculator() {
        assertEquals(listOf(25.0 to 1, 15.0 to 1), PlateCalculator.calculate(100.0, 20.0, Units.KG)!!.perSide)
        assertEquals(listOf(45.0 to 2), PlateCalculator.calculate(225.0, 45.0, Units.LB)!!.perSide)
        assertEquals(1.0, PlateCalculator.calculate(61.0, 20.0, Units.KG)!!.remainder, 1e-6)
        assertNull(PlateCalculator.calculate(15.0, 20.0, Units.KG))
    }

    @Test
    fun streak_countsConsecutiveDays() {
        assertEquals(3, Stats.currentStreak(listOf(millis(today), millis(today.minusDays(1)), millis(today.minusDays(2))), today))
        assertEquals(2, Stats.currentStreak(listOf(millis(today.minusDays(1)), millis(today.minusDays(2))), today))
        assertEquals(0, Stats.currentStreak(listOf(millis(today.minusDays(2))), today))
        assertEquals(1, Stats.currentStreak(listOf(millis(today, 8), millis(today, 18)), today))
    }

    @Test
    fun weeklyStats() {
        fun w(id: Long, d: LocalDate, vol: Double) =
            WorkoutEntity(workoutId = id, userId = "u", name = "W", startedAt = millis(d), finishedAt = millis(d), totalVolumeKg = vol)
        val workouts = listOf(w(1, today, 1000.0), w(2, LocalDate.of(2026, 11, 2), 500.0), w(3, LocalDate.of(2026, 11, 1), 700.0))
        assertEquals(2, Stats.workoutsThisWeek(workouts, today))
        val weekly = Stats.weeklyVolume(workouts, 8, today)
        assertEquals(8, weekly.size)
        assertEquals(1500.0, weekly.last().second, 0.0)
        assertEquals(700.0, weekly[6].second, 0.0)
    }

    @Test
    fun personalRecords_useBestEstimated1rm() {
        fun s(id: Long, wid: Long, ex: Long, kg: Double, reps: Int, at: Long) = CompletedSet(id, wid, ex, "Ex$ex", 1, kg, reps, at)
        val prs = Stats.personalRecords(listOf(s(1, 1, 10, 100.0, 5, 1000), s(2, 2, 10, 90.0, 10, 2000), s(3, 2, 20, 50.0, 8, 2000)))
        val bench = prs.first { it.exerciseId == 10L }
        assertEquals(120.0, bench.bestE1rmKg, 0.01) // 90 × 10 beats 100 × 5
        assertEquals(90.0, bench.bestWeightKg, 0.0)
        assertTrue(prs.first().achievedAt == 2000L)
    }

    @Test
    fun levelsAndFormatting() {
        assertEquals(1, Stats.levelForXp(0))
        assertEquals(2, Stats.levelForXp(260))
        assertEquals("1h 05m", Formatters.duration(3900))
        assertEquals("01:05", Formatters.clock(65))
    }
}
