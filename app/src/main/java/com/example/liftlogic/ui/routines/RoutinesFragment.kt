package com.example.liftlogic.ui.routines

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentRoutinesBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.workout.WorkoutLauncher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

/** "Workouts" tab: the user's routines plus the built-in programs. */
class RoutinesFragment : BaseFragment<FragmentRoutinesBinding>(FragmentRoutinesBinding::inflate) {

    private val adapter = RoutinesAdapter { summary ->
        findNavController().navigate(
            R.id.routineDetailFragment,
            bundleOf(RoutineDetailFragment.ARG_ROUTINE_ID to summary.routine.routineId)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.routineList.layoutManager = LinearLayoutManager(requireContext())
        binding.routineList.adapter = adapter

        binding.startEmptyButton.setOnClickListener { WorkoutLauncher.startEmpty(this) }
        binding.newRoutineFab.setOnClickListener { findNavController().navigate(R.id.routineEditorFragment) }

        val routines = app.session.userId.flatMapLatest { id ->
            if (id == null) emptyFlow() else app.routineRepo.observeRoutines(id)
        }
        observe(routines) { all ->
            val mine = all.filter { it.routine.ownerUserId != null }
            val programs = all.filter { it.routine.ownerUserId == null }
            val rows = buildList {
                add(RoutineRow.Header(R.string.header_my_routines))
                if (mine.isEmpty()) add(RoutineRow.Empty(R.string.empty_routines))
                else mine.forEach { add(RoutineRow.Item(it)) }
                add(RoutineRow.Header(R.string.header_programs))
                programs.forEach { add(RoutineRow.Item(it)) }
            }
            adapter.submitList(rows)
        }
    }

    override fun onDestroyView() {
        binding.routineList.adapter = null
        super.onDestroyView()
    }
}
