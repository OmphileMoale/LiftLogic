package com.example.liftlogic.data
import android.util.Log

import androidx.room.withTransaction
import com.example.liftlogic.data.db.AppDatabase
import com.example.liftlogic.data.db.BadgeEntity
import com.example.liftlogic.data.db.CompletedSet
import com.example.liftlogic.data.db.WorkoutEntity
import com.example.liftlogic.data.db.WorkoutExerciseEntity
import com.example.liftlogic.data.db.WorkoutSetEntity
import com.example.liftlogic.util.OneRepMax
import com.example.liftlogic.util.Stats
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.Calendar

data class FinishResult(
    val workoutId: Long,
    val xpEarned: Int,
    val prCount: Int,
    val newBadges: List<String>
)

class WorkoutRepository(private val db: AppDatabase, scope: CoroutineScope) {

    private val workoutDao = db.workoutDao()
    private val exerciseDao = db.exerciseDao()
    private val routineDao = db.routineDao()
    private val userDao = db.userDao()
    private val badgeDao = db.badgeDao()

    /**
     * Edits typed into the active workout are written one after another through this queue,
     * so fast typing can never save an older value after a newer one.
     */
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (write in writes) {
                runCatching { write() }
            }
        }
    }

    fun enqueue(write: suspend () -> Unit) {
        writes.trySend(write)
    }

    /** Waits until every queued edit has been saved. */
    suspend fun flush() {
        val done = CompletableDeferred<Unit>()
        writes.send { done.complete(Unit) }
        done.await()
    }

    // ------------------------------ reading ------------------------------

    fun observeActive(userId: String) = workoutDao.observeActiveWorkout(userId)

    suspend fun getActive(userId: String) = workoutDao.getActiveWorkout(userId)

    fun observeFinished(userId: String) = workoutDao.observeFinishedWorkouts(userId)

    fun observeWorkout(id: Long) = workoutDao.observeWorkout(id)

    suspend fun getWorkout(id: Long) = workoutDao.getWorkout(id)

    fun observeWorkoutExercises(workoutId: Long) = workoutDao.observeWorkoutExercises(workoutId)

    fun observeSets(workoutId: Long) = workoutDao.observeSets(workoutId)

    suspend fun getWorkoutExercises(workoutId: Long) = workoutDao.getWorkoutExercises(workoutId)

    suspend fun getSets(workoutId: Long) = workoutDao.getSets(workoutId)

    fun observeCompletedSets(userId: String) = workoutDao.observeCompletedSets(userId)

    fun observeCompletedSetsForExercise(userId: String, exerciseId: Long) =
        workoutDao.observeCompletedSetsForExercise(userId, exerciseId)

    fun observeBadges(userId: String) = badgeDao.observeBadges(userId)

    /** The sets from the most recent finished workout that included this exercise ("previous" column). */
    suspend fun previousSets(userId: String, exerciseId: Long): List<CompletedSet> {
        val all = workoutDao.getCompletedSetsForExercise(userId, exerciseId)
        val lastWorkout = all.firstOrNull()?.workoutId ?: return emptyList()
        return all.filter { it.workoutId == lastWorkout }.sortedBy { it.setNumber }
    }

    // ------------------------------ starting ------------------------------

    suspend fun startEmpty(userId: String, name: String): Long =
        workoutDao.insertWorkout(WorkoutEntity(userId = userId, name = name, startedAt = System.currentTimeMillis()))

    suspend fun startFromRoutine(userId: String, routineId: Long): Long = db.withTransaction {
        val routine = routineDao.getRoutine(routineId)
        val items = routineDao.getRoutineExercises(routineId)
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(
                userId = userId,
                name = routine?.name ?: "Workout",
                routineId = routineId,
                startedAt = System.currentTimeMillis()
            )
        )
        items.forEachIndexed { index, item ->
            val weId = workoutDao.insertWorkoutExercise(
                WorkoutExerciseEntity(
                    workoutId = workoutId,
                    exerciseId = item.item.exerciseId,
                    exerciseName = item.exerciseName,
                    muscleGroup = item.muscleGroup,
                    position = index,
                    restSec = item.item.restSec,
                    targetReps = item.item.targetReps
                )
            )
            workoutDao.insertSets((1..item.item.targetSets.coerceAtLeast(1)).map { n ->
                WorkoutSetEntity(workoutExerciseId = weId, workoutId = workoutId, setNumber = n)
            })
        }
        workoutId
    }

    /** Starts a new workout with the same exercises, sets, weights and reps as an old one. */
    suspend fun repeatWorkout(userId: String, oldWorkoutId: Long): Long = db.withTransaction {
        val old = workoutDao.getWorkout(oldWorkoutId)
        val exercises = workoutDao.getWorkoutExercises(oldWorkoutId)
        val sets = workoutDao.getSets(oldWorkoutId).groupBy { it.workoutExerciseId }
        val workoutId = workoutDao.insertWorkout(
            WorkoutEntity(
                userId = userId,
                name = old?.name ?: "Workout",
                routineId = old?.routineId,
                startedAt = System.currentTimeMillis()
            )
        )
        exercises.forEachIndexed { index, we ->
            val newId = workoutDao.insertWorkoutExercise(we.copy(id = 0, workoutId = workoutId, position = index))
            val oldSets = sets[we.id].orEmpty().ifEmpty { listOf(WorkoutSetEntity(workoutExerciseId = 0, workoutId = 0, setNumber = 1)) }
            workoutDao.insertSets(oldSets.mapIndexed { i, s ->
                WorkoutSetEntity(
                    workoutExerciseId = newId,
                    workoutId = workoutId,
                    setNumber = i + 1,
                    weightKg = s.weightKg,
                    reps = s.reps
                )
            })
        }
        workoutId
    }

    // ------------------------------ editing ------------------------------

    suspend fun addExercises(workoutId: Long, exerciseIds: List<Long>, defaultRestSec: Int) {
        flush()
        db.withTransaction {
            var position = (workoutDao.maxPosition(workoutId) ?: -1) + 1
            for (id in exerciseIds) {
                val ex = exerciseDao.getById(id) ?: continue
                val weId = workoutDao.insertWorkoutExercise(
                    WorkoutExerciseEntity(
                        workoutId = workoutId,
                        exerciseId = ex.exerciseId,
                        exerciseName = ex.name,
                        muscleGroup = ex.muscleGroup,
                        position = position++,
                        restSec = defaultRestSec
                    )
                )
                workoutDao.insertSet(WorkoutSetEntity(workoutExerciseId = weId, workoutId = workoutId, setNumber = 1))
            }
        }
    }

    fun removeExercise(workoutExerciseId: Long) = enqueue { workoutDao.deleteWorkoutExercise(workoutExerciseId) }

    /** Adds a set to an exercise, copying the weight and reps of the last set. */
    fun addSet(workoutExerciseId: Long, workoutId: Long) = enqueue {
        val existing = workoutDao.getSetsForExercise(workoutExerciseId)
        val last = existing.maxByOrNull { it.setNumber }
        workoutDao.insertSet(
            WorkoutSetEntity(
                workoutExerciseId = workoutExerciseId,
                workoutId = workoutId,
                setNumber = (last?.setNumber ?: 0) + 1,
                weightKg = last?.weightKg,
                reps = last?.reps
            )
        )
    }

    fun deleteSet(set: WorkoutSetEntity) = enqueue {
        db.withTransaction {
            workoutDao.deleteSet(set.setId)
            workoutDao.getSetsForExercise(set.workoutExerciseId).forEachIndexed { index, s ->
                if (s.setNumber != index + 1) workoutDao.updateSet(s.copy(setNumber = index + 1))
            }
        }
    }

    fun updateWeight(setId: Long, weightKg: Double?) = enqueue { workoutDao.updateSetWeight(setId, weightKg) }

    fun updateReps(setId: Long, reps: Int?) = enqueue { workoutDao.updateSetReps(setId, reps) }

    fun updateSet(setId: Long, weightKg: Double?, reps: Int?, completed: Boolean) =
        enqueue { workoutDao.updateSetValues(setId, weightKg, reps, completed) }

    fun rename(workoutId: Long, name: String) = enqueue { workoutDao.renameWorkout(workoutId, name.trim()) }

    suspend fun discard(workoutId: Long) {
        flush()
        workoutDao.deleteWorkout(workoutId)
    }

    suspend fun deleteWorkout(workoutId: Long) = workoutDao.deleteWorkout(workoutId)

    // ------------------------------ finishing ------------------------------

    /**
     * Finishes the workout: removes unfinished sets, marks personal records, awards XP and badges.
     * Returns null when no set was completed (nothing to save).
     */
    suspend fun finish(userId: String, workoutId: Long): FinishResult? {
        Log.d("WorkoutRepository", "finish: userId=$userId workoutId=$workoutId")
        flush()
        return db.withTransaction {
            val workout = workoutDao.getWorkout(workoutId) ?: return@withTransaction null
            val completed = workoutDao.getSets(workoutId).filter { it.completed && it.reps != null }
            if (completed.isEmpty()) return@withTransaction null

            workoutDao.deleteIncompleteSets(workoutId)
            workoutDao.deleteEmptyExercises(workoutId)

            // Personal records: best estimated 1RM beats everything logged before
            val exerciseIdOf = workoutDao.getWorkoutExercises(workoutId).associate { it.id to it.exerciseId }
            val previousBest = workoutDao.getCompletedSets(userId)
                .groupBy { it.exerciseId }
                .mapValues { (_, sets) -> sets.maxOf { OneRepMax.estimate(it.weightKg, it.reps) } }

            var prCount = 0
            completed.groupBy { exerciseIdOf[it.workoutExerciseId] }.forEach { (exerciseId, sets) ->
                if (exerciseId == null) return@forEach
                val best = previousBest[exerciseId] ?: return@forEach // first time: nothing to beat yet
                val top = sets.filter { (it.weightKg ?: 0.0) > 0.0 }
                    .maxByOrNull { OneRepMax.estimate(it.weightKg ?: 0.0, it.reps ?: 0) } ?: return@forEach
                if (OneRepMax.estimate(top.weightKg ?: 0.0, top.reps ?: 0) > best + 0.01) {
                    workoutDao.updateSet(top.copy(isPr = true))
                    prCount++
                }
            }

            val volume = completed.sumOf { (it.weightKg ?: 0.0) * (it.reps ?: 0) }
            val now = System.currentTimeMillis()
            val xp = 50 + (volume / 100).toInt().coerceAtMost(200) + prCount * 25

            workoutDao.updateWorkout(
                workout.copy(
                    finishedAt = now,
                    durationSec = ((now - workout.startedAt) / 1000).toInt(),
                    totalVolumeKg = volume,
                    totalSets = completed.size,
                    xpEarned = xp,
                    prCount = prCount
                )
            )
            userDao.addXp(userId, xp)

            val newBadges = awardBadges(userId, workout.startedAt, volume, prCount, now)
            FinishResult(workoutId, xp, prCount, newBadges)
        }
    }

    private suspend fun awardBadges(userId: String, startedAt: Long, volume: Double, prCount: Int, now: Long): List<String> {
        val finished = workoutDao.getFinishedWorkouts(userId)
        val streak = Stats.currentStreak(finished.mapNotNull { it.finishedAt })
        val startHour = Calendar.getInstance().apply { timeInMillis = startedAt }.get(Calendar.HOUR_OF_DAY)

        val earned = buildList {
            if (finished.isNotEmpty()) add(Badge.FIRST_WORKOUT)
            if (finished.size >= 10) add(Badge.WORKOUTS_10)
            if (finished.size >= 50) add(Badge.WORKOUTS_50)
            if (streak >= 3) add(Badge.STREAK_3)
            if (streak >= 7) add(Badge.STREAK_7)
            if (volume >= 10_000) add(Badge.VOLUME_10K)
            if (prCount > 0) add(Badge.FIRST_PR)
            if (startHour < 7) add(Badge.EARLY_BIRD)
        }
        return earned.filter { badgeDao.insert(BadgeEntity(userId = userId, badgeKey = it.key, unlockedAt = now)) != -1L }
            .map { it.key }
    }
}
