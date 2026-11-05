package com.example.liftlogic.ui.workout

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.liftlogic.AppContainer
import com.example.liftlogic.R
import com.example.liftlogic.ui.appContainer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

/**
 * Starts a workout from any screen. If one is already in progress the user can resume it
 * or discard it and start the new one (only one workout can be active at a time).
 */
object WorkoutLauncher {

    fun start(fragment: Fragment, create: suspend (app: AppContainer, userId: String) -> Long) {
        val app = fragment.appContainer
        val userId = app.session.currentUserId ?: return
        val scope = fragment.viewLifecycleOwner.lifecycleScope

        scope.launch {
            val active = app.workoutRepo.getActive(userId)
            if (active == null) {
                create(app, userId)
                openActive(fragment)
                return@launch
            }
            MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.dialog_workout_in_progress)
                .setMessage(fragment.getString(R.string.dialog_workout_in_progress_msg, active.name))
                .setPositiveButton(R.string.action_resume) { _, _ -> openActive(fragment) }
                .setNegativeButton(R.string.action_discard_and_start) { _, _ ->
                    scope.launch {
                        app.restTimer.skip()
                        app.workoutRepo.discard(active.workoutId)
                        create(app, userId)
                        openActive(fragment)
                    }
                }
                .setNeutralButton(R.string.action_cancel, null)
                .show()
        }
    }

    fun startEmpty(fragment: Fragment) = start(fragment) { app, userId ->
        app.workoutRepo.startEmpty(userId, fragment.getString(R.string.default_workout_name))
    }

    fun startRoutine(fragment: Fragment, routineId: Long) = start(fragment) { app, userId ->
        app.workoutRepo.startFromRoutine(userId, routineId)
    }

    fun repeat(fragment: Fragment, workoutId: Long) = start(fragment) { app, userId ->
        app.workoutRepo.repeatWorkout(userId, workoutId)
    }

    fun openActive(fragment: Fragment) {
        if (!fragment.isAdded) return
        fragment.findNavController().navigate(R.id.activeWorkoutFragment, null, navOptions { launchSingleTop = true })
    }
}
