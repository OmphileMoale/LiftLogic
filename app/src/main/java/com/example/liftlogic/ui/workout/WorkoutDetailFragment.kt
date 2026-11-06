package com.example.liftlogic.ui.workout

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.R
import com.example.liftlogic.data.db.WorkoutEntity
import com.example.liftlogic.databinding.FragmentWorkoutDetailBinding
import com.example.liftlogic.databinding.ItemDetailExerciseBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.confirm
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.showTextInputDialog
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Units
import kotlinx.coroutines.flow.combine

/** A finished workout: every exercise and set, with options to repeat, save as routine or delete. */
class WorkoutDetailFragment : BaseFragment<FragmentWorkoutDetailBinding>(FragmentWorkoutDetailBinding::inflate) {

    private val workoutId: Long get() = requireArguments().getLong(ARG_WORKOUT_ID)
    private var workout: WorkoutEntity? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.appBar.toolbar
        setupToolbar(toolbar, R.string.title_workout)
        toolbar.inflateMenu(R.menu.menu_workout_detail)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_save_routine -> {
                    saveAsRoutine()
                    true
                }
                R.id.action_delete -> {
                    confirm(R.string.action_delete_workout, getString(R.string.msg_delete_workout), R.string.action_delete) {
                        launchUi {
                            app.workoutRepo.deleteWorkout(workoutId)
                            findNavController().navigateUp()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        binding.metricDuration.label.setText(R.string.label_duration)
        binding.metricVolume.label.setText(R.string.label_total_volume)
        binding.metricPrs.label.setText(R.string.label_records)
        binding.repeatButton.setOnClickListener { WorkoutLauncher.repeat(this, workoutId) }

        val data = combine(
            app.workoutRepo.observeWorkout(workoutId),
            app.workoutRepo.observeWorkoutExercises(workoutId),
            app.workoutRepo.observeSets(workoutId),
            app.currentUser
        ) { w, exercises, sets, user -> Detail(w, exercises, sets, user?.units ?: Units.KG) }

        observe(data) { detail ->
            val w = detail.workout ?: return@observe
            workout = w
            toolbar.title = w.name
            binding.dateText.text = Formatters.dateTime(w.finishedAt ?: w.startedAt)
            binding.metricDuration.value.text = Formatters.duration(w.durationSec)
            binding.metricVolume.value.text = Units.volume(w.totalVolumeKg, detail.unit)
            binding.metricPrs.value.text = w.prCount.toString()

            val container = binding.exerciseContainer
            container.removeAllViews()
            val setsByExercise = detail.sets.groupBy { it.workoutExerciseId }
            for (ex in detail.exercises) {
                val card = ItemDetailExerciseBinding.inflate(layoutInflater, container, false)
                card.name.text = ex.exerciseName
                card.setsText.text = setsByExercise[ex.id].orEmpty().sortedBy { it.setNumber }.joinToString("\n") { s ->
                    val weight = s.weightKg?.let { Units.weight(it, detail.unit) } ?: getString(R.string.label_bodyweight_short)
                    val line = "${s.setNumber}   $weight × ${s.reps ?: 0}"
                    if (s.isPr) "$line   ${getString(R.string.label_pr_marker)}" else line
                }
                container.addView(card.root)
            }
        }
    }

    private data class Detail(
        val workout: WorkoutEntity?,
        val exercises: List<com.example.liftlogic.data.db.WorkoutExerciseEntity>,
        val sets: List<com.example.liftlogic.data.db.WorkoutSetEntity>,
        val unit: String
    )

    private fun saveAsRoutine() {
        val userId = app.session.currentUserId ?: return
        val w = workout ?: return
        showTextInputDialog(R.string.action_save_as_routine, R.string.label_routine_name, initial = w.name) { name ->
            launchUi {
                app.routineRepo.createFromWorkout(userId, w.workoutId, name.ifBlank { w.name })
                toast(R.string.text_routine_saved)
            }
        }
    }

    companion object {
        const val ARG_WORKOUT_ID = "workoutId"
    }
}
