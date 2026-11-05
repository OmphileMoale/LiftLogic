package com.example.liftlogic.ui.workout

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.R
import com.example.liftlogic.data.Badge
import com.example.liftlogic.data.db.WorkoutEntity
import com.example.liftlogic.databinding.FragmentWorkoutSummaryBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.showTextInputDialog
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Stats
import com.example.liftlogic.util.Units
import com.google.android.material.chip.Chip

/** Shown right after finishing a workout: stats, new records, XP and badges. */
class WorkoutSummaryFragment : BaseFragment<FragmentWorkoutSummaryBinding>(FragmentWorkoutSummaryBinding::inflate) {

    private val workoutId: Long get() = requireArguments().getLong(ARG_WORKOUT_ID)
    private var workout: WorkoutEntity? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.metricDuration.label.setText(R.string.label_duration)
        binding.metricVolume.label.setText(R.string.label_total_volume)
        binding.metricSets.label.setText(R.string.label_sets)
        binding.metricPrs.label.setText(R.string.label_records)
        binding.metricXp.label.setText(R.string.label_xp_earned)
        binding.metricLevel.label.setText(R.string.stat_level)

        showBadges(arguments?.getStringArray(ARG_BADGES).orEmpty().toList())

        binding.doneButton.setOnClickListener { goHome() }
        binding.shareButton.setOnClickListener { share() }
        binding.saveRoutineButton.setOnClickListener { saveAsRoutine() }

        // Back from the summary also returns Home
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })

        launchUi {
            val w = app.workoutRepo.getWorkout(workoutId) ?: return@launchUi
            workout = w
            val unit = app.unit
            val b = bindingOrNull ?: return@launchUi
            b.workoutName.text = w.name
            b.workoutDate.text = Formatters.dateTime(w.finishedAt ?: w.startedAt)
            b.metricDuration.value.text = Formatters.duration(w.durationSec)
            b.metricVolume.value.text = Units.volume(w.totalVolumeKg, unit)
            b.metricSets.value.text = w.totalSets.toString()
            b.metricPrs.value.text = w.prCount.toString()
            b.metricXp.value.text = getString(R.string.format_xp_earned, w.xpEarned)
            b.metricLevel.value.text = Stats.levelForXp(app.currentUser.value?.xp ?: 0).toString()
            b.saveRoutineButton.visibleIf(w.routineId == null)

            // List the sets that set a new personal record
            val names = app.workoutRepo.getWorkoutExercises(workoutId).associate { it.id to it.exerciseName }
            val prs = app.workoutRepo.getSets(workoutId).filter { it.isPr }
            b.prsTitle.visibleIf(prs.isNotEmpty())
            b.prsText.visibleIf(prs.isNotEmpty())
            b.prsText.text = prs.joinToString("\n") {
                val set = getString(R.string.format_set, Units.number(Units.display(it.weightKg ?: 0.0, unit)), it.reps ?: 0)
                "${names[it.workoutExerciseId].orEmpty()} — $set"
            }
        }
    }

    private fun showBadges(keys: List<String>) {
        val badges = keys.mapNotNull { Badge.fromKey(it) }
        binding.badgesTitle.visibleIf(badges.isNotEmpty())
        binding.badgesGroup.visibleIf(badges.isNotEmpty())
        binding.badgesGroup.removeAllViews()
        badges.forEach { badge ->
            binding.badgesGroup.addView(Chip(requireContext()).apply {
                setText(badge.title)
                setChipIconResource(badge.icon)
                isClickable = false
            })
        }
    }

    private fun goHome() {
        val nav = findNavController()
        if (!nav.popBackStack(R.id.homeFragment, false)) nav.navigate(R.id.homeFragment)
    }

    private fun saveAsRoutine() {
        val userId = app.session.currentUserId ?: return
        val w = workout ?: return
        showTextInputDialog(R.string.action_save_as_routine, R.string.label_routine_name, initial = w.name) { name ->
            launchUi {
                app.routineRepo.createFromWorkout(userId, w.workoutId, name.ifBlank { w.name })
                toast(R.string.text_routine_saved)
                bindingOrNull?.saveRoutineButton?.visibleIf(false)
            }
        }
    }

    private fun share() {
        val w = workout ?: return
        val text = getString(
            R.string.share_workout_text,
            w.name,
            Formatters.duration(w.durationSec),
            Units.volume(w.totalVolumeKg, app.unit),
            w.totalSets,
            w.prCount
        )
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, getString(R.string.action_share_summary)))
    }

    companion object {
        const val ARG_WORKOUT_ID = "workoutId"
        const val ARG_BADGES = "badges"
    }
}
