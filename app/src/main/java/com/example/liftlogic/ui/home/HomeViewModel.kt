package com.example.liftlogic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.liftlogic.AppContainer
import com.example.liftlogic.data.Experience
import com.example.liftlogic.data.db.RoutineSummary
import com.example.liftlogic.data.db.UserEntity
import com.example.liftlogic.data.db.WorkoutEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class HomeState(
    val user: UserEntity? = null,
    val active: WorkoutEntity? = null,
    val finished: List<WorkoutEntity> = emptyList(),
    val suggested: RoutineSummary? = null,
    val suggestedLastDone: Long? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(app: AppContainer) : ViewModel() {

    val state: StateFlow<HomeState> = app.session.userId
        .flatMapLatest { id ->
            if (id == null) flowOf(HomeState())
            else combine(
                app.userRepo.observeUser(id),
                app.workoutRepo.observeActive(id),
                app.workoutRepo.observeFinished(id),
                app.routineRepo.observeRoutines(id)
            ) { user, active, finished, routines ->
                val lastDone = lastDoneByRoutine(finished)
                val suggested = suggest(user, routines, lastDone)
                HomeState(user, active, finished, suggested, suggested?.let { lastDone[it.routine.routineId] })
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private fun lastDoneByRoutine(finished: List<WorkoutEntity>): Map<Long, Long> =
        finished.filter { it.routineId != null && it.finishedAt != null }
            .groupBy { it.routineId!! }
            .mapValues { (_, list) -> list.maxOf { it.finishedAt!! } }

    /**
     * Suggests the routine that was done least recently: the user's own routines first,
     * otherwise a built-in program that suits their experience.
     */
    private fun suggest(user: UserEntity?, routines: List<RoutineSummary>, lastDone: Map<Long, Long>): RoutineSummary? {
        val own = routines.filter { it.routine.ownerUserId != null && it.exerciseCount > 0 }
        val programs = routines.filter { it.routine.ownerUserId == null }
        val pool = own.ifEmpty {
            if (Experience.fromKey(user?.experience) == Experience.BEGINNER) {
                programs.filter { it.routine.name.startsWith("Full Body") }.ifEmpty { programs }
            } else {
                programs
            }
        }
        return pool.minByOrNull { lastDone[it.routine.routineId] ?: 0L }
    }
}
