package com.example.liftlogic.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liftlogic.AppContainer
import com.example.liftlogic.data.FinishResult
import com.example.liftlogic.data.db.CompletedSet
import com.example.liftlogic.data.db.WorkoutEntity
import com.example.liftlogic.data.db.WorkoutExerciseEntity
import com.example.liftlogic.data.db.WorkoutSetEntity
import com.example.liftlogic.util.Units
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One row in the active-workout list. */
sealed class WorkoutRow {
    abstract val id: String

    data class Header(val exercise: WorkoutExerciseEntity, val unit: String, val setCount: Int) : WorkoutRow() {
        override val id: String get() = "h${exercise.id}"
    }

    data class SetRow(
        val set: WorkoutSetEntity,
        val previous: CompletedSet?,
        val unit: String,
        val targetReps: String,
        val restSec: Int
    ) : WorkoutRow() {
        override val id: String get() = "s${set.setId}"
    }

    data class AddSet(val workoutExerciseId: Long, val workoutId: Long) : WorkoutRow() {
        override val id: String get() = "a$workoutExerciseId"
    }
}

/** loaded = false until the database has answered, so "no workout" can be told apart from "not loaded yet". */
data class ActiveLoad(val loaded: Boolean, val workout: WorkoutEntity?)

@OptIn(ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel(private val app: AppContainer) : ViewModel() {

    val active: StateFlow<ActiveLoad> = app.session.userId
        .flatMapLatest { id ->
            if (id == null) flowOf(ActiveLoad(true, null))
            else app.workoutRepo.observeActive(id).map { ActiveLoad(true, it) }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ActiveLoad(false, null))

    private val workoutId = active.map { it.workout?.workoutId }.distinctUntilChanged()

    /** exerciseId -> sets from the last time the user did that exercise */
    private val previous = MutableStateFlow<Map<Long, List<CompletedSet>>>(emptyMap())

    val rows: StateFlow<List<WorkoutRow>> = workoutId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else combine(
                app.workoutRepo.observeWorkoutExercises(id),
                app.workoutRepo.observeSets(id),
                previous,
                app.currentUser
            ) { exercises, sets, prev, user ->
                buildRows(id, exercises, sets, prev, user?.units ?: Units.KG)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Look up "previous" performance whenever new exercises appear in the workout
        viewModelScope.launch {
            workoutId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else app.workoutRepo.observeWorkoutExercises(id)
            }.collect { exercises -> loadPrevious(exercises.map { it.exerciseId }.distinct()) }
        }
    }

    private suspend fun loadPrevious(exerciseIds: List<Long>) {
        val userId = app.session.currentUserId ?: return
        val missing = exerciseIds.filter { it !in previous.value }
        if (missing.isEmpty()) return
        val updated = previous.value.toMutableMap()
        for (id in missing) updated[id] = app.workoutRepo.previousSets(userId, id)
        previous.value = updated
    }

    private fun buildRows(
        workoutId: Long,
        exercises: List<WorkoutExerciseEntity>,
        sets: List<WorkoutSetEntity>,
        previous: Map<Long, List<CompletedSet>>,
        unit: String
    ): List<WorkoutRow> {
        val setsByExercise = sets.groupBy { it.workoutExerciseId }
        val rows = mutableListOf<WorkoutRow>()
        for (ex in exercises) {
            val exSets = setsByExercise[ex.id].orEmpty().sortedBy { it.setNumber }
            rows += WorkoutRow.Header(ex, unit, exSets.size)
            val prev = previous[ex.exerciseId].orEmpty()
            exSets.forEach { s ->
                rows += WorkoutRow.SetRow(s, prev.getOrNull(s.setNumber - 1), unit, ex.targetReps, ex.restSec)
            }
            rows += WorkoutRow.AddSet(ex.id, workoutId)
        }
        return rows
    }

    // ------------------------------ edits ------------------------------

    fun updateWeight(setId: Long, text: String, unit: String) {
        val kg = Units.parse(text)?.let { Units.toKg(it, unit) }
        app.workoutRepo.updateWeight(setId, kg)
    }

    fun updateReps(setId: Long, text: String) {
        app.workoutRepo.updateReps(setId, text.trim().toIntOrNull())
    }

    /** Returns false if the set can't be completed yet (no reps entered). */
    fun setCompleted(row: WorkoutRow.SetRow, completed: Boolean, weightText: String, repsText: String): Boolean {
        val reps = repsText.trim().toIntOrNull()
        if (completed && (reps == null || reps <= 0)) return false
        val kg = Units.parse(weightText)?.let { Units.toKg(it, row.unit) }
        app.workoutRepo.updateSet(row.set.setId, kg, reps, completed)
        if (completed) {
            app.restTimer.start(row.restSec.takeIf { it > 0 } ?: (app.currentUser.value?.defaultRestSec ?: 90))
        }
        return true
    }

    /** Copies last time's weight and reps into this set. */
    fun usePrevious(row: WorkoutRow.SetRow) {
        val prev = row.previous ?: return
        app.workoutRepo.updateWeight(row.set.setId, prev.weightKg)
        app.workoutRepo.updateReps(row.set.setId, prev.reps)
    }

    fun addSet(row: WorkoutRow.AddSet) = app.workoutRepo.addSet(row.workoutExerciseId, row.workoutId)

    fun deleteSet(row: WorkoutRow.SetRow) = app.workoutRepo.deleteSet(row.set)

    fun removeExercise(workoutExerciseId: Long) = app.workoutRepo.removeExercise(workoutExerciseId)

    fun addExercises(ids: LongArray) {
        val workoutId = active.value.workout?.workoutId ?: return
        viewModelScope.launch {
            app.workoutRepo.addExercises(workoutId, ids.toList(), app.currentUser.value?.defaultRestSec ?: 90)
        }
    }

    fun rename(name: String) {
        val workoutId = active.value.workout?.workoutId ?: return
        if (name.isNotBlank()) app.workoutRepo.rename(workoutId, name)
    }

    suspend fun discard() {
        val workoutId = active.value.workout?.workoutId ?: return
        app.restTimer.skip()
        app.notifications.cancelRest()
        app.workoutRepo.discard(workoutId)
    }

    suspend fun finish(): FinishResult? {
        val userId = app.session.currentUserId ?: return null
        val workoutId = active.value.workout?.workoutId ?: return null
        val result = app.workoutRepo.finish(userId, workoutId)
        if (result != null) {
            app.restTimer.skip()
            app.notifications.cancelRest()
        }
        return result
    }

    // Rest timer controls
    val restState get() = app.restTimer.state
    fun adjustRest(delta: Int) = app.restTimer.adjust(delta)
    fun skipRest() = app.restTimer.skip()
}
