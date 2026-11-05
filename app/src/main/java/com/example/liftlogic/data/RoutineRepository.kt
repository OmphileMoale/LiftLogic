package com.example.liftlogic.data

import androidx.room.withTransaction
import com.example.liftlogic.data.db.AppDatabase
import com.example.liftlogic.data.db.RoutineEntity
import com.example.liftlogic.data.db.RoutineExerciseEntity

/** One exercise line while creating/editing a routine. */
data class RoutineItemDraft(
    val key: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    var targetSets: Int = 3,
    var targetReps: String = "8-12",
    var restSec: Int = 90
)

class RoutineRepository(private val db: AppDatabase) {
    private val routineDao = db.routineDao()
    private val workoutDao = db.workoutDao()

    fun observeRoutines(userId: String) = routineDao.observeRoutines(userId)

    fun observeRoutine(id: Long) = routineDao.observeRoutine(id)

    fun observeRoutineExercises(id: Long) = routineDao.observeRoutineExercises(id)

    suspend fun getRoutine(id: Long) = routineDao.getRoutine(id)

    suspend fun getRoutineExercises(id: Long) = routineDao.getRoutineExercises(id)

    /** Creates (routineId == null) or updates a routine with its exercise list. */
    suspend fun saveRoutine(
        userId: String,
        routineId: Long?,
        name: String,
        description: String,
        items: List<RoutineItemDraft>
    ): Long = db.withTransaction {
        val id = if (routineId == null) {
            routineDao.insertRoutine(RoutineEntity(ownerUserId = userId, name = name.trim(), description = description.trim()))
        } else {
            val existing = routineDao.getRoutine(routineId)
            if (existing != null) routineDao.updateRoutine(existing.copy(name = name.trim(), description = description.trim()))
            routineId
        }
        routineDao.replaceItems(id, items.mapIndexed { index, d ->
            RoutineExerciseEntity(
                routineId = id,
                exerciseId = d.exerciseId,
                position = index,
                targetSets = d.targetSets.coerceIn(1, 20),
                targetReps = d.targetReps.ifBlank { "8-12" },
                restSec = d.restSec.coerceIn(0, 600)
            )
        })
        id
    }

    suspend fun deleteRoutine(id: Long) = routineDao.deleteRoutine(id)

    /** Copies a routine (e.g. a built-in program) into the user's own routines. */
    suspend fun duplicate(userId: String, routineId: Long, newName: String): Long {
        val routine = routineDao.getRoutine(routineId) ?: return -1
        val items = routineDao.getRoutineExercises(routineId).map {
            RoutineItemDraft(
                key = it.item.id,
                exerciseId = it.item.exerciseId,
                exerciseName = it.exerciseName,
                muscleGroup = it.muscleGroup,
                targetSets = it.item.targetSets,
                targetReps = it.item.targetReps,
                restSec = it.item.restSec
            )
        }
        return saveRoutine(userId, null, newName, routine.description, items)
    }

    /** Turns a finished workout into a reusable routine. */
    suspend fun createFromWorkout(userId: String, workoutId: Long, name: String): Long {
        val exercises = workoutDao.getWorkoutExercises(workoutId)
        val sets = workoutDao.getSets(workoutId).groupBy { it.workoutExerciseId }
        val items = exercises.map { we ->
            val exSets = sets[we.id].orEmpty()
            val reps = exSets.mapNotNull { it.reps }
            RoutineItemDraft(
                key = we.id,
                exerciseId = we.exerciseId,
                exerciseName = we.exerciseName,
                muscleGroup = we.muscleGroup,
                targetSets = exSets.size.coerceAtLeast(1),
                targetReps = if (reps.isEmpty()) "8-12" else if (reps.min() == reps.max()) "${reps.min()}" else "${reps.min()}-${reps.max()}",
                restSec = we.restSec
            )
        }
        return saveRoutine(userId, null, name, "", items)
    }
}
