package com.example.liftlogic.ui.workout

import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentActiveWorkoutBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.appViewModels
import com.example.liftlogic.ui.confirm
import com.example.liftlogic.ui.exercises.ExerciseDetailFragment
import com.example.liftlogic.ui.exercises.ExerciseListFragment
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.showTextInputDialog
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Formatters
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.delay

/** The live workout logger. */
class ActiveWorkoutFragment : BaseFragment<FragmentActiveWorkoutBinding>(FragmentActiveWorkoutBinding::inflate),
    WorkoutRowCallbacks {

    private val viewModel: ActiveWorkoutViewModel by appViewModels { ActiveWorkoutViewModel(it) }
    private lateinit var adapter: ActiveWorkoutAdapter
    private var finishing = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.appBar.toolbar
        setupToolbar(toolbar, R.string.title_active_workout)
        toolbar.inflateMenu(R.menu.menu_active_workout)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_rename -> {
                    showTextInputDialog(R.string.action_rename, R.string.label_workout_name, viewModel.active.value.workout?.name.orEmpty()) {
                        viewModel.rename(it)
                    }
                    true
                }
                R.id.action_discard -> {
                    confirmDiscard()
                    true
                }
                else -> false
            }
        }

        adapter = ActiveWorkoutAdapter(this)
        binding.setList.layoutManager = LinearLayoutManager(requireContext())
        binding.setList.adapter = adapter
        binding.setList.itemAnimator = null // avoids flicker while typing
        attachSwipeToDelete()

        binding.addExerciseButton.setOnClickListener { findNavController().navigate(R.id.exercisePickerFragment) }
        binding.finishButton.setOnClickListener { confirmFinish() }
        binding.restMinus.setOnClickListener { viewModel.adjustRest(-15) }
        binding.restPlus.setOnClickListener { viewModel.adjustRest(15) }
        binding.restSkip.setOnClickListener { viewModel.skipRest() }

        observe(viewModel.active) { load ->
            val workout = load.workout
            if (load.loaded && workout == null && !finishing) {
                // Workout was finished or discarded
                if (findNavController().currentDestination?.id == R.id.activeWorkoutFragment) findNavController().popBackStack()
                return@observe
            }
            toolbar.title = workout?.name ?: getString(R.string.title_active_workout)
            updateClock()
        }

        observe(viewModel.rows) { rows ->
            adapter.submitList(rows)
            binding.emptyBox.visibleIf(rows.isEmpty() && viewModel.active.value.loaded)
        }

        observe(viewModel.restState) { rest ->
            binding.restCard.visibleIf(rest.running)
            if (rest.running) {
                binding.restText.text = getString(R.string.format_rest, Formatters.clock(rest.remainingSec))
                binding.restProgress.max = rest.totalSec.coerceAtLeast(1)
                binding.restProgress.progress = rest.remainingSec
            }
        }

        // Exercises chosen in the picker
        val handle = findNavController().currentBackStackEntry?.savedStateHandle
        if (handle != null) {
            observe(handle.getStateFlow<LongArray?>(ExerciseListFragment.RESULT_PICKED_IDS, null)) { ids ->
                if (ids != null) {
                    viewModel.addExercises(ids)
                    handle.remove<LongArray>(ExerciseListFragment.RESULT_PICKED_IDS)
                }
            }
        }

        // Workout clock in the toolbar
        launchUi {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    updateClock()
                    delay(1000)
                }
            }
        }
    }

    private fun updateClock() {
        val b = bindingOrNull ?: return
        val workout = viewModel.active.value.workout ?: return
        val elapsed = ((System.currentTimeMillis() - workout.startedAt) / 1000).toInt()
        b.appBar.toolbar.subtitle = Formatters.clock(elapsed)
    }

    private fun attachSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun getSwipeDirs(rv: RecyclerView, holder: RecyclerView.ViewHolder): Int =
                if (holder is ActiveWorkoutAdapter.SetVH) super.getSwipeDirs(rv, holder) else 0

            override fun onMove(rv: RecyclerView, a: RecyclerView.ViewHolder, b: RecyclerView.ViewHolder) = false

            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                val row = adapter.rowAt(holder.bindingAdapterPosition) as? WorkoutRow.SetRow
                if (row != null) {
                    viewModel.deleteSet(row)
                    toast(R.string.text_set_deleted)
                } else {
                    adapter.notifyItemChanged(holder.bindingAdapterPosition)
                }
            }

            override fun onChildDraw(c: Canvas, rv: RecyclerView, holder: RecyclerView.ViewHolder, dX: Float, dY: Float, state: Int, active: Boolean) {
                // Fade the row while it is swiped away
                holder.itemView.alpha = 1f - (kotlin.math.abs(dX) / rv.width).coerceIn(0f, 0.8f)
                super.onChildDraw(c, rv, holder, dX, dY, state, active)
            }

            override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) {
                holder.itemView.alpha = 1f
                super.clearView(rv, holder)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.setList)
    }

    // ---------------------------- row callbacks ----------------------------

    override fun onWeightChanged(row: WorkoutRow.SetRow, text: String) = viewModel.updateWeight(row.set.setId, text, row.unit)

    override fun onRepsChanged(row: WorkoutRow.SetRow, text: String) = viewModel.updateReps(row.set.setId, text)

    override fun onSetCompleted(row: WorkoutRow.SetRow, completed: Boolean, weightText: String, repsText: String): Boolean {
        val ok = viewModel.setCompleted(row, completed, weightText, repsText)
        if (!ok) toast(R.string.error_enter_reps)
        return ok
    }

    override fun onUsePrevious(row: WorkoutRow.SetRow) = viewModel.usePrevious(row)

    override fun onAddSet(row: WorkoutRow.AddSet) = viewModel.addSet(row)

    override fun onExerciseMenu(row: WorkoutRow.Header, anchor: View) {
        PopupMenu(requireContext(), anchor).apply {
            menuInflater.inflate(R.menu.menu_workout_exercise, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_exercise_info -> {
                        findNavController().navigate(
                            R.id.exerciseDetailFragment,
                            bundleOf(ExerciseDetailFragment.ARG_EXERCISE_ID to row.exercise.exerciseId)
                        )
                        true
                    }
                    R.id.action_remove_exercise -> {
                        confirm(R.string.action_remove_exercise, getString(R.string.msg_remove_exercise, row.exercise.exerciseName), R.string.action_remove) {
                            viewModel.removeExercise(row.exercise.id)
                        }
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    // ---------------------------- finish / discard ----------------------------

    private fun confirmFinish() {
        val sets = viewModel.rows.value.filterIsInstance<WorkoutRow.SetRow>()
        val done = sets.count { it.set.completed }
        if (done == 0) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.title_no_sets_done)
                .setMessage(R.string.msg_no_sets_done)
                .setPositiveButton(R.string.action_keep_going, null)
                .setNegativeButton(R.string.action_discard_workout) { _, _ -> discard() }
                .show()
            return
        }
        val unfinished = sets.size - done
        val message = if (unfinished > 0) {
            resources.getQuantityString(R.plurals.msg_finish_unfinished, unfinished, unfinished)
        } else {
            getString(R.string.msg_finish_workout)
        }
        confirm(R.string.title_finish_workout, message, R.string.action_finish) { finish() }
    }

    private fun finish() {
        finishing = true
        binding.finishButton.isEnabled = false
        launchUi {
            val result = viewModel.finish()
            if (result == null) {
                finishing = false
                bindingOrNull?.finishButton?.isEnabled = true
                return@launchUi
            }
            findNavController().navigate(
                R.id.action_activeWorkout_to_summary,
                bundleOf(
                    WorkoutSummaryFragment.ARG_WORKOUT_ID to result.workoutId,
                    WorkoutSummaryFragment.ARG_BADGES to result.newBadges.toTypedArray()
                )
            )
        }
    }

    private fun confirmDiscard() {
        confirm(R.string.action_discard_workout, getString(R.string.msg_discard_workout), R.string.action_discard) { discard() }
    }

    private fun discard() {
        launchUi { viewModel.discard() } // the screen closes when the active workout disappears
    }

    override fun onDestroyView() {
        binding.setList.adapter = null
        super.onDestroyView()
    }
}
