package com.example.liftlogic.ui.home

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.liftlogic.MainActivity
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentHomeBinding
import com.example.liftlogic.databinding.ItemStatCardBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.appViewModels
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.ui.workout.WorkoutDetailFragment
import com.example.liftlogic.ui.workout.WorkoutHistoryAdapter
import com.example.liftlogic.ui.workout.WorkoutLauncher
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Stats
import kotlinx.coroutines.delay
import java.util.Calendar

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by appViewModels { HomeViewModel(it) }

    private val recentAdapter = WorkoutHistoryAdapter { workout ->
        findNavController().navigate(
            R.id.workoutDetailFragment,
            bundleOf(WorkoutDetailFragment.ARG_WORKOUT_ID to workout.workoutId)
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.greetingText.setText(
            when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> R.string.greeting_morning
                in 12..16 -> R.string.greeting_afternoon
                else -> R.string.greeting_evening
            }
        )

        setupStatCard(binding.statStreak, R.drawable.ic_whatshot, R.color.stat_streak, R.string.label_day_streak)
        setupStatCard(binding.statWeek, R.drawable.ic_event, R.color.lift_green, R.string.label_this_week)
        setupStatCard(binding.statLevel, R.drawable.ic_emoji_events, R.color.stat_level, R.string.stat_level)

        binding.recentList.layoutManager = LinearLayoutManager(requireContext())
        binding.recentList.adapter = recentAdapter

        val main = requireActivity() as MainActivity
        binding.quickStartButton.setOnClickListener { WorkoutLauncher.startEmpty(this) }
        binding.browseRoutinesButton.setOnClickListener { main.navigateToTab(R.id.routinesFragment) }
        binding.resumeButton.setOnClickListener { WorkoutLauncher.openActive(this) }
        binding.activeCard.setOnClickListener { WorkoutLauncher.openActive(this) }
        binding.seeAllButton.setOnClickListener { findNavController().navigate(R.id.historyFragment) }

        observe(viewModel.state) { state ->
            val user = state.user
            binding.nameText.text = user?.name ?: getString(R.string.default_athlete)
            recentAdapter.unit = user?.units ?: "kg"

            // Stats
            val finishedTimes = state.finished.mapNotNull { it.finishedAt }
            binding.statStreak.value.text = Stats.currentStreak(finishedTimes).toString()
            val thisWeek = Stats.workoutsThisWeek(state.finished)
            val goal = user?.weeklyGoal ?: 3
            binding.statWeek.value.text = getString(R.string.format_fraction, thisWeek, goal)
            binding.statLevel.value.text = Stats.levelForXp(user?.xp ?: 0).toString()

            binding.weeklyText.text = getString(R.string.format_weekly_progress, thisWeek, goal)
            binding.weeklyProgress.max = goal
            binding.weeklyProgress.setProgressCompat(thisWeek.coerceAtMost(goal), true)

            // Suggested routine
            val suggested = state.suggested
            binding.suggestedName.text = suggested?.routine?.name ?: getString(R.string.default_workout_name)
            binding.suggestedDetail.text = if (suggested == null) {
                getString(R.string.text_no_routines_yet)
            } else {
                val count = resources.getQuantityString(R.plurals.exercise_count, suggested.exerciseCount, suggested.exerciseCount)
                val last = state.suggestedLastDone?.let {
                    getString(R.string.format_last_done, DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.DAY_IN_MILLIS))
                } ?: getString(R.string.text_not_done_yet)
                "$count · $last"
            }
            binding.suggestedStartButton.setOnClickListener {
                if (suggested == null) WorkoutLauncher.startEmpty(this)
                else WorkoutLauncher.startRoutine(this, suggested.routine.routineId)
            }

            // Recent workouts
            val recent = state.finished.take(3)
            recentAdapter.submitList(recent)
            binding.recentList.visibleIf(recent.isNotEmpty())
            binding.emptyHistory.visibleIf(recent.isEmpty())
            binding.seeAllButton.visibleIf(state.finished.size > 3)

            binding.activeCard.visibleIf(state.active != null)
            updateActiveTimer()
        }

        // Live clock on the "workout in progress" card
        launchUi {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    updateActiveTimer()
                    delay(1000)
                }
            }
        }
    }

    private fun updateActiveTimer() {
        val b = bindingOrNull ?: return
        val active = viewModel.state.value.active ?: return
        val elapsed = ((System.currentTimeMillis() - active.startedAt) / 1000).toInt()
        b.activeName.text = getString(R.string.format_name_and_time, active.name, Formatters.clock(elapsed))
    }

    private fun setupStatCard(card: ItemStatCardBinding, @DrawableRes icon: Int, @ColorRes colorRes: Int, @StringRes label: Int) {
        val color = ContextCompat.getColor(requireContext(), colorRes)
        card.icon.setImageResource(icon)
        card.icon.imageTintList = ColorStateList.valueOf(color)
        card.iconBackground.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(color, 51))
        card.label.setText(label)
        card.value.text = "0"
    }

    override fun onDestroyView() {
        binding.recentList.adapter = null
        super.onDestroyView()
    }
}
