package com.example.liftlogic.util

import com.example.liftlogic.data.db.CompletedSet
import com.example.liftlogic.data.db.WorkoutEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** Estimated one-rep max (Epley formula). */
object OneRepMax {
    fun estimate(weight: Double, reps: Int): Double = when {
        reps <= 0 -> 0.0
        reps == 1 -> weight
        else -> weight * (1 + reps / 30.0)
    }

    /** Typical percentages of 1RM and the rep range they correspond to. */
    val percentages = listOf(
        100 to "1", 95 to "2", 90 to "4", 85 to "6", 80 to "8", 75 to "10", 70 to "12", 65 to "15", 60 to "20"
    )
}

/** Works out which plates to load on EACH side of the bar. */
object PlateCalculator {
    private val kgPlates = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    private val lbPlates = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5)

    data class Result(val perSide: List<Pair<Double, Int>>, val remainder: Double)

    fun calculate(target: Double, bar: Double, unit: String): Result? {
        if (target < bar) return null
        var perSide = (target - bar) / 2
        val plates = if (unit == Units.LB) lbPlates else kgPlates
        val out = mutableListOf<Pair<Double, Int>>()
        for (plate in plates) {
            val count = (perSide / plate + 1e-9).toInt()
            if (count > 0) {
                out += plate to count
                perSide -= count * plate
            }
        }
        return Result(out, perSide * 2)
    }
}

/** A best performance on one exercise. */
data class PersonalRecord(
    val exerciseId: Long,
    val exerciseName: String,
    val bestE1rmKg: Double,
    val bestWeightKg: Double,
    val bestReps: Int,
    val achievedAt: Long
)

/** Pure calculations used by Home, Progress, Profile and the workout summary. */
object Stats {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun toDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** Consecutive days (ending today, or yesterday) with at least one finished workout. */
    fun currentStreak(finishedAt: List<Long>, today: LocalDate = LocalDate.now()): Int {
        val days = finishedAt.map { toDate(it) }.toSet()
        var day = when {
            today in days -> today
            today.minusDays(1) in days -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        while (day in days) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    fun startOfWeek(today: LocalDate = LocalDate.now()): LocalDate =
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    fun workoutsThisWeek(workouts: List<WorkoutEntity>, today: LocalDate = LocalDate.now()): Int {
        val start = startOfWeek(today)
        return workouts.count { w -> w.finishedAt?.let { !toDate(it).isBefore(start) } == true }
    }

    /** Total volume per week for the last [weeks] weeks, oldest first. */
    fun weeklyVolume(workouts: List<WorkoutEntity>, weeks: Int = 8, today: LocalDate = LocalDate.now()): List<Pair<LocalDate, Double>> {
        val thisWeek = startOfWeek(today)
        return (weeks - 1 downTo 0).map { i ->
            val start = thisWeek.minusWeeks(i.toLong())
            val end = start.plusWeeks(1)
            val volume = workouts.filter { w ->
                val d = w.finishedAt?.let { toDate(it) } ?: return@filter false
                !d.isBefore(start) && d.isBefore(end)
            }.sumOf { it.totalVolumeKg }
            start to volume
        }
    }

    /** Best estimated 1RM per exercise, most recently trained first. */
    fun personalRecords(sets: List<CompletedSet>): List<PersonalRecord> =
        sets.groupBy { it.exerciseId }.map { (exerciseId, list) ->
            val best = list.maxBy { OneRepMax.estimate(it.weightKg, it.reps) }
            PersonalRecord(
                exerciseId = exerciseId,
                exerciseName = list.first().exerciseName,
                bestE1rmKg = OneRepMax.estimate(best.weightKg, best.reps),
                bestWeightKg = best.weightKg,
                bestReps = best.reps,
                achievedAt = list.maxOf { it.finishedAt }
            )
        }.sortedByDescending { it.achievedAt }

    /** Best estimated 1RM per workout for one exercise, oldest first (for the exercise chart). */
    fun e1rmHistory(sets: List<CompletedSet>): List<Pair<Long, Double>> =
        sets.groupBy { it.workoutId }
            .map { (_, list) -> list.first().finishedAt to list.maxOf { OneRepMax.estimate(it.weightKg, it.reps) } }
            .sortedBy { it.first }

    fun levelForXp(xp: Int): Int = 1 + xp / XP_PER_LEVEL

    fun xpIntoLevel(xp: Int): Int = xp % XP_PER_LEVEL

    const val XP_PER_LEVEL = 250
}
