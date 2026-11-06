package com.example.liftlogic.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liftlogic.AppContainer
import com.example.liftlogic.data.RoutineItemDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Collections

/**
 * Holds the routine being edited. The list is kept here (not in the screen) so it survives
 * rotation and the trip to the exercise picker and back.
 */
class RoutineEditorViewModel(private val app: AppContainer, private val routineId: Long?) : ViewModel() {

    val items: MutableList<RoutineItemDraft> = mutableListOf()

    /** Increases whenever exercises are added, removed or loaded, so the screen refreshes. */
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    private val _loaded = MutableStateFlow<Pair<String, String>?>(null)
    /** Name and description of an existing routine (emitted once after loading). */
    val loaded: StateFlow<Pair<String, String>?> = _loaded.asStateFlow()

    var name: String = ""
    var description: String = ""

    private var nextKey = -1L

    init {
        if (routineId != null) {
            viewModelScope.launch {
                val routine = app.routineRepo.getRoutine(routineId)
                name = routine?.name.orEmpty()
                description = routine?.description.orEmpty()
                items.clear()
                items += app.routineRepo.getRoutineExercises(routineId).map {
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
                _loaded.value = name to description
                _version.value++
            }
        }
    }

    fun addExercises(ids: LongArray) {
        viewModelScope.launch {
            val rest = app.currentUser.value?.defaultRestSec ?: 90
            for (id in ids) {
                val ex = app.exerciseRepo.getExercise(id) ?: continue
                items += RoutineItemDraft(
                    key = nextKey--,
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.name,
                    muscleGroup = ex.muscleGroup,
                    restSec = rest
                )
            }
            _version.value++
        }
    }

    fun remove(position: Int) {
        if (position !in items.indices) return
        items.removeAt(position)
        _version.value++
    }

    fun move(from: Int, to: Int) {
        if (from !in items.indices || to !in items.indices) return
        Collections.swap(items, from, to)
    }

    /** Returns the new routine id, or null when the input is not valid. */
    suspend fun save(): Long? {
        val userId = app.session.currentUserId ?: return null
        if (name.isBlank() || items.isEmpty()) return null
        return app.routineRepo.saveRoutine(userId, routineId, name, description, items.toList())
    }
}
