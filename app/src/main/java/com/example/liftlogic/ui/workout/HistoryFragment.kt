package com.example.liftlogic.ui.workout

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentHistoryBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Units
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

/** Every finished workout, newest first. */
class HistoryFragment : BaseFragment<FragmentHistoryBinding>(FragmentHistoryBinding::inflate) {

    private val adapter = WorkoutHistoryAdapter { workout ->
        findNavController().navigate(
            R.id.workoutDetailFragment,
            bundleOf(WorkoutDetailFragment.ARG_WORKOUT_ID to workout.workoutId)
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.appBar.toolbar, R.string.title_history)

        binding.historyList.layoutManager = LinearLayoutManager(requireContext())
        binding.historyList.adapter = adapter

        val workouts = app.session.userId.flatMapLatest { id ->
            if (id == null) emptyFlow() else app.workoutRepo.observeFinished(id)
        }
        observe(combine(workouts, app.currentUser) { list, user -> list to (user?.units ?: Units.KG) }) { (list, unit) ->
            adapter.unit = unit
            adapter.submitList(list)
            binding.emptyText.visibleIf(list.isEmpty())
            binding.summaryText.text = getString(
                R.string.format_history_summary,
                resources.getQuantityString(R.plurals.workout_count, list.size, list.size),
                Units.volume(list.sumOf { it.totalVolumeKg }, unit)
            )
        }
    }

    override fun onDestroyView() {
        binding.historyList.adapter = null
        super.onDestroyView()
    }
}
