package com.example.liftlogic.ui.routines

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.data.db.RoutineExerciseDetail
import com.example.liftlogic.databinding.FragmentRoutineDetailBinding
import com.example.liftlogic.databinding.ItemRoutineExerciseBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.confirm
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.showTextInputDialog
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.ui.workout.WorkoutLauncher
import com.example.liftlogic.util.Formatters

class RoutineDetailFragment : BaseFragment<FragmentRoutineDetailBinding>(FragmentRoutineDetailBinding::inflate) {

    private val routineId: Long get() = requireArguments().getLong(ARG_ROUTINE_ID)
    private val adapter = RoutineExerciseAdapter()
    private var routineName = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.appBar.toolbar
        setupToolbar(toolbar)
        toolbar.inflateMenu(R.menu.menu_routine_detail)

        binding.exerciseList.layoutManager = LinearLayoutManager(requireContext())
        binding.exerciseList.adapter = adapter
        binding.startButton.setOnClickListener { WorkoutLauncher.startRoutine(this, routineId) }

        observe(app.routineRepo.observeRoutine(routineId)) { routine ->
            if (routine == null) {
                // The routine was deleted
                if (findNavController().currentDestination?.id == R.id.routineDetailFragment) findNavController().navigateUp()
                return@observe
            }
            routineName = routine.name
            toolbar.title = routine.name
            binding.description.text = routine.description
            binding.description.visibleIf(routine.description.isNotBlank())

            val isProgram = routine.ownerUserId == null
            binding.programLabel.visibleIf(isProgram)
            toolbar.menu.findItem(R.id.action_edit).isVisible = !isProgram
            toolbar.menu.findItem(R.id.action_delete).isVisible = !isProgram
            toolbar.menu.findItem(R.id.action_duplicate).setTitle(
                if (isProgram) R.string.action_copy_to_my_routines else R.string.action_duplicate
            )
        }

        observe(app.routineRepo.observeRoutineExercises(routineId)) { items ->
            adapter.items = items
            binding.emptyText.visibleIf(items.isEmpty())
            binding.startButton.isEnabled = items.isNotEmpty()
            val sets = items.sumOf { it.item.targetSets }
            // Rough estimate: ~45 s per set plus the rest between sets
            val seconds = items.sumOf { it.item.targetSets * (45 + it.item.restSec) }
            binding.summary.text = getString(
                R.string.format_routine_summary,
                resources.getQuantityString(R.plurals.exercise_count, items.size, items.size),
                resources.getQuantityString(R.plurals.set_count, sets, sets),
                Formatters.duration(seconds)
            )
        }

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_edit -> {
                    findNavController().navigate(R.id.routineEditorFragment, bundleOf(RoutineEditorFragment.ARG_ROUTINE_ID to routineId))
                    true
                }
                R.id.action_duplicate -> {
                    duplicate()
                    true
                }
                R.id.action_delete -> {
                    confirm(R.string.title_delete_routine, getString(R.string.msg_delete_routine, routineName), R.string.action_delete) {
                        launchUi { app.routineRepo.deleteRoutine(routineId) }
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun duplicate() {
        val userId = app.session.currentUserId ?: return
        showTextInputDialog(R.string.action_copy_to_my_routines, R.string.label_routine_name, initial = routineName) { name ->
            launchUi {
                val newId = app.routineRepo.duplicate(userId, routineId, name.ifBlank { routineName })
                if (newId > 0) {
                    toast(R.string.text_routine_copied)
                    findNavController().navigate(R.id.routineEditorFragment, bundleOf(RoutineEditorFragment.ARG_ROUTINE_ID to newId))
                }
            }
        }
    }

    override fun onDestroyView() {
        binding.exerciseList.adapter = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ROUTINE_ID = "routineId"
    }
}

/** Read-only list of the exercises in a routine. */
class RoutineExerciseAdapter : RecyclerView.Adapter<RoutineExerciseAdapter.VH>() {

    var items: List<RoutineExerciseDetail> = emptyList()
        @android.annotation.SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class VH(val binding: ItemRoutineExerciseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemRoutineExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]
        val context = holder.itemView.context
        holder.binding.position.text = (position + 1).toString()
        holder.binding.name.text = d.exerciseName
        holder.binding.detail.text = context.getString(
            R.string.format_routine_item,
            d.item.targetSets,
            d.item.targetReps,
            Formatters.clock(d.item.restSec)
        )
    }
}
