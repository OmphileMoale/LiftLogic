package com.example.liftlogic.ui.exercises

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liftlogic.AppContainer
import com.example.liftlogic.data.MuscleGroup
import com.example.liftlogic.data.db.ExerciseEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ExerciseListViewModel(app: AppContainer) : ViewModel() {

    private val query = MutableStateFlow("")
    private val _muscle = MutableStateFlow<MuscleGroup?>(null)
    val muscle: StateFlow<MuscleGroup?> = _muscle.asStateFlow()

    /** Ids ticked in picker mode, in the order they were picked. */
    private val _selected = MutableStateFlow<List<Long>>(emptyList())
    val selected: StateFlow<List<Long>> = _selected.asStateFlow()

    private val library = app.session.userId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else app.exerciseRepo.observeLibrary(id)
    }

    val exercises: StateFlow<List<ExerciseEntity>> = combine(library, query, _muscle) { all, q, muscle ->
        val text = q.trim()
        all.filter { ex ->
            (muscle == null || ex.muscleGroup == muscle.key) &&
                (text.isEmpty() || ex.name.contains(text, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(text: String) {
        query.value = text
    }

    fun setMuscle(group: MuscleGroup?) {
        _muscle.value = group
    }

    fun toggle(id: Long) {
        val current = _selected.value
        _selected.value = if (id in current) current - id else current + id
    }
}
