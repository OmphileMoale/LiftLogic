package com.example.liftlogic.ui.exercises

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.R
import com.example.liftlogic.data.Equipment
import com.example.liftlogic.data.MuscleGroup
import com.example.liftlogic.data.db.CompletedSet
import com.example.liftlogic.data.db.ExerciseEntity
import com.example.liftlogic.databinding.FragmentExerciseDetailBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.confirm
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.OneRepMax
import com.example.liftlogic.util.Stats
import com.example.liftlogic.util.Units
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

/** Exercise instructions plus the user's own history, best lifts and 1RM chart for it. */
class ExerciseDetailFragment : BaseFragment<FragmentExerciseDetailBinding>(FragmentExerciseDetailBinding::inflate) {

    private val exerciseId: Long get() = requireArguments().getLong(ARG_EXERCISE_ID)
    private var exercise: ExerciseEntity? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.appBar.toolbar
        setupToolbar(toolbar)
        toolbar.inflateMenu(R.menu.menu_edit_delete)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    findNavController().navigate(R.id.exerciseEditorFragment, bundleOf(ExerciseEditorFragment.ARG_EXERCISE_ID to exerciseId))
                    true
                }
                R.id.action_delete -> {
                    val ex = exercise ?: return@setOnMenuItemClickListener true
                    confirm(R.string.title_delete_exercise, getString(R.string.msg_delete_exercise, ex.name), R.string.action_delete) {
                        launchUi {
                            app.exerciseRepo.delete(ex)
                            findNavController().navigateUp()
                        }
                    }
                    true
                }
                else -> false
            }
        }

        binding.metricE1rm.label.setText(R.string.label_est_1rm)
        binding.metricBest.label.setText(R.string.label_heaviest)
        binding.metricSessions.label.setText(R.string.label_sessions)

        observe(app.exerciseRepo.observeExercise(exerciseId)) { ex ->
            exercise = ex ?: return@observe
            toolbar.title = ex.name
            binding.muscleTag.setText(MuscleGroup.fromKey(ex.muscleGroup).label)
            binding.equipmentTag.setText(Equipment.fromKey(ex.equipment).label)
            binding.instructions.text = ex.instructions.ifBlank { getString(R.string.text_no_instructions) }
            // Only the user's own exercises can be edited or deleted
            toolbar.menu.findItem(R.id.action_edit).isVisible = ex.isCustom
            toolbar.menu.findItem(R.id.action_delete).isVisible = ex.isCustom
        }

        val history = app.session.userId.flatMapLatest { id ->
            if (id == null) emptyFlow() else app.workoutRepo.observeCompletedSetsForExercise(id, exerciseId)
        }
        observe(combine(history, app.currentUser) { sets, user -> sets to (user?.units ?: Units.KG) }) { (sets, unit) ->
            showHistory(sets, unit)
        }
    }

    private fun showHistory(sets: List<CompletedSet>, unit: String) {
        if (sets.isEmpty()) {
            binding.metricE1rm.value.text = "—"
            binding.metricBest.value.text = "—"
            binding.metricSessions.value.text = "0"
            binding.historyText.setText(R.string.empty_exercise_history)
            binding.e1rmChart.visibleIf(false)
            binding.chartEmpty.visibleIf(true)
            return
        }

        val best = sets.maxOf { OneRepMax.estimate(it.weightKg, it.reps) }
        val heaviest = sets.maxBy { it.weightKg }
        binding.metricE1rm.value.text = Units.weight(best, unit)
        binding.metricBest.value.text = getString(R.string.format_set, Units.number(Units.display(heaviest.weightKg, unit)), heaviest.reps)
        binding.metricSessions.value.text = sets.map { it.workoutId }.distinct().size.toString()

        val points = Stats.e1rmHistory(sets)
        binding.e1rmChart.visibleIf(points.size >= 2)
        binding.chartEmpty.visibleIf(points.size < 2)
        binding.e1rmChart.setData(
            points.map { Units.fromKg(it.second, unit).toFloat() },
            points.map { Formatters.shortDate(it.first) }
        ) { Units.number(it.toDouble()) }

        // Most recent 8 sessions, one line each
        binding.historyText.text = sets.groupBy { it.workoutId }.values
            .take(8)
            .joinToString("\n") { session ->
                val date = Formatters.shortDate(session.first().finishedAt)
                val list = session.sortedBy { it.setNumber }.joinToString(", ") {
                    getString(R.string.format_set, Units.number(Units.display(it.weightKg, unit)), it.reps)
                }
                "$date — $list"
            }
    }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
    }
}
