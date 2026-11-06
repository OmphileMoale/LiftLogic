package com.example.liftlogic.ui.progress

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.AppContainer
import com.example.liftlogic.R
import com.example.liftlogic.data.db.BodyWeightEntity
import com.example.liftlogic.data.db.CompletedSet
import com.example.liftlogic.data.db.WorkoutEntity
import com.example.liftlogic.databinding.FragmentProgressBinding
import com.example.liftlogic.databinding.ItemRecordBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.appViewModels
import com.example.liftlogic.ui.exercises.ExerciseDetailFragment
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.showTextInputDialog
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Stats
import com.example.liftlogic.util.Units
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class ProgressState(
    val unit: String = Units.KG,
    val workouts: List<WorkoutEntity> = emptyList(),
    val sets: List<CompletedSet> = emptyList(),
    val bodyWeights: List<BodyWeightEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModel(private val app: AppContainer) : ViewModel() {

    val state: StateFlow<ProgressState> = app.session.userId
        .flatMapLatest { id ->
            if (id == null) flowOf(ProgressState())
            else combine(
                app.workoutRepo.observeFinished(id),
                app.workoutRepo.observeCompletedSets(id),
                app.bodyWeightRepo.observeHistory(id),
                app.currentUser
            ) { workouts, sets, weights, user -> ProgressState(user?.units ?: Units.KG, workouts, sets, weights) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun logBodyWeight(value: Double) {
        val userId = app.session.currentUserId ?: return
        val unit = state.value.unit
        viewModelScope.launch { app.bodyWeightRepo.log(userId, Units.toKg(value, unit)) }
    }
}

class ProgressFragment : BaseFragment<FragmentProgressBinding>(FragmentProgressBinding::inflate) {

    private val viewModel: ProgressViewModel by appViewModels { ProgressViewModel(it) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.metricWorkouts.label.setText(R.string.label_workouts)
        binding.metricVolume.label.setText(R.string.label_total_volume)
        binding.metricMonth.label.setText(R.string.label_this_month)

        binding.historyButton.setOnClickListener { findNavController().navigate(R.id.historyFragment) }
        binding.bodyWeightCard.setOnClickListener { findNavController().navigate(R.id.bodyWeightFragment) }
        binding.logWeightButton.setOnClickListener { askBodyWeight() }

        observe(viewModel.state) { render(it) }
    }

    private fun askBodyWeight() {
        val title = getString(R.string.format_body_weight_hint, viewModel.state.value.unit)
        showTextInputDialog(R.string.action_log_weight, R.string.label_weight, numeric = true) { text ->
            val value = Units.parse(text)
            if (value != null && value > 0) {
                viewModel.logBodyWeight(value)
                toast(R.string.text_weight_logged)
            } else {
                toast(title)
            }
        }
    }

    private fun render(state: ProgressState) {
        val unit = state.unit

        // Overview
        binding.metricWorkouts.value.text = state.workouts.size.toString()
        binding.metricVolume.value.text = Units.volume(state.workouts.sumOf { it.totalVolumeKg }, unit)
        val monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        binding.metricMonth.value.text = state.workouts.count { (it.finishedAt ?: 0) >= monthStart }.toString()

        // Weekly volume (last 8 weeks)
        val weeks = Stats.weeklyVolume(state.workouts)
        binding.weeklyChart.setData(
            weeks.map { Units.fromKg(it.second, unit).toFloat() },
            weeks.map { "${it.first.dayOfMonth}/${it.first.monthValue}" }
        )
        binding.weeklyTitle.text = getString(R.string.format_weekly_volume_title, unit)

        // Body weight
        val weights = state.bodyWeights
        val latest = weights.lastOrNull()
        binding.bodyWeightText.text = latest?.let { Units.weight(it.weightKg, unit) } ?: getString(R.string.text_no_body_weight)
        binding.bodyWeightChange.visibleIf(weights.size >= 2)
        if (weights.size >= 2 && latest != null) {
            val change = Units.fromKg(latest.weightKg - weights.first().weightKg, unit)
            val sign = if (change > 0) "+" else ""
            binding.bodyWeightChange.text = getString(R.string.format_weight_change, "$sign${Units.number(change)} $unit", Formatters.shortDate(weights.first().recordedAt))
        }
        binding.bodyWeightChart.visibleIf(weights.size >= 2)
        binding.bodyWeightChart.setData(
            weights.map { Units.fromKg(it.weightKg, unit).toFloat() },
            weights.map { Formatters.shortDate(it.recordedAt) }
        ) { Units.number(it.toDouble()) }

        // Personal records
        val records = Stats.personalRecords(state.sets).take(8)
        binding.recordsEmpty.visibleIf(records.isEmpty())
        val container = binding.recordsContainer
        container.removeAllViews()
        records.forEach { pr ->
            val row = ItemRecordBinding.inflate(layoutInflater, container, false)
            row.name.text = pr.exerciseName
            row.detail.text = getString(
                R.string.format_record_detail,
                getString(R.string.format_set, Units.number(Units.display(pr.bestWeightKg, unit)), pr.bestReps),
                Formatters.shortDate(pr.achievedAt)
            )
            row.e1rm.text = getString(R.string.format_e1rm, Units.weight(pr.bestE1rmKg, unit))
            row.root.setOnClickListener {
                findNavController().navigate(R.id.exerciseDetailFragment, bundleOf(ExerciseDetailFragment.ARG_EXERCISE_ID to pr.exerciseId))
            }
            container.addView(row.root)
        }
    }
}
