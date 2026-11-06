package com.example.liftlogic.ui.auth

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.R
import com.example.liftlogic.data.Experience
import com.example.liftlogic.data.FitnessGoal
import com.example.liftlogic.databinding.FragmentProfileSetupBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Units

/**
 * Onboarding after sign-up (goal, experience, height, weight, weekly target),
 * also used as "Edit profile" from Settings.
 */
class ProfileSetupFragment : BaseFragment<FragmentProfileSetupBinding>(FragmentProfileSetupBinding::inflate) {

    private val onboarding: Boolean get() = arguments?.getBoolean(ARG_ONBOARDING) ?: false

    private val goalChips = mapOf(
        R.id.chip_goal_muscle to FitnessGoal.BUILD_MUSCLE,
        R.id.chip_goal_strength to FitnessGoal.GET_STRONGER,
        R.id.chip_goal_fat to FitnessGoal.LOSE_FAT,
        R.id.chip_goal_fit to FitnessGoal.STAY_FIT
    )
    private val experienceChips = mapOf(
        R.id.chip_exp_beginner to Experience.BEGINNER,
        R.id.chip_exp_intermediate to Experience.INTERMEDIATE,
        R.id.chip_exp_advanced to Experience.ADVANCED
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.appBar.toolbar

        if (onboarding) {
            toolbar.setTitle(R.string.title_profile_setup)
            toolbar.navigationIcon = null
            binding.saveButton.setText(R.string.action_continue)
            binding.skipButton.visibleIf(true)
        } else {
            setupToolbar(toolbar, R.string.settings_edit_profile)
            binding.introText.visibleIf(false)
        }

        binding.weeklyGoalSlider.addOnChangeListener { _, value, _ -> updateWeeklyLabel(value.toInt()) }
        updateWeeklyLabel(binding.weeklyGoalSlider.value.toInt())

        loadUser(fillFields = savedInstanceState == null)

        binding.saveButton.setOnClickListener { save() }
        binding.skipButton.setOnClickListener { finish() }
    }

    /** Weight unit of this user (read from the database, so it is right straight after sign-up). */
    private var unit = Units.KG

    private fun loadUser(fillFields: Boolean) {
        val userId = app.session.currentUserId ?: return
        launchUi {
            val user = app.userRepo.getUser(userId) ?: return@launchUi
            val b = bindingOrNull ?: return@launchUi
            unit = user.units
            b.weightLayout.hint = getString(R.string.format_body_weight_hint, unit)
            if (!onboarding) {
                b.emailText.visibleIf(true)
                b.emailText.text = getString(R.string.format_email_locked, user.email)
            }
            if (!fillFields) return@launchUi
            b.nameInput.setText(user.name)
            b.goalGroup.check(goalChips.entries.first { it.value == FitnessGoal.fromKey(user.goal) }.key)
            b.experienceGroup.check(experienceChips.entries.first { it.value == Experience.fromKey(user.experience) }.key)
            user.heightCm?.let { b.heightInput.setText(Units.number(it)) }
            b.weeklyGoalSlider.value = user.weeklyGoal.coerceIn(1, 7).toFloat()
            updateWeeklyLabel(user.weeklyGoal)
        }
    }

    private fun updateWeeklyLabel(count: Int) {
        bindingOrNull?.weeklyGoalLabel?.text = resources.getQuantityString(R.plurals.weekly_goal_label, count, count)
    }

    private fun save() {
        val userId = app.session.currentUserId ?: return
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.error_name_empty)
            return
        }
        binding.nameLayout.error = null

        val goal = goalChips[binding.goalGroup.checkedChipId] ?: FitnessGoal.BUILD_MUSCLE
        val experience = experienceChips[binding.experienceGroup.checkedChipId] ?: Experience.BEGINNER
        val height = Units.parse(binding.heightInput.text)?.takeIf { it in 50.0..260.0 }
        val bodyWeight = Units.parse(binding.weightInput.text)?.takeIf { it > 0 }
        val weekly = binding.weeklyGoalSlider.value.toInt()

        launchUi {
            app.userRepo.update(userId) {
                it.copy(name = name, goal = goal.key, experience = experience.key, heightCm = height ?: it.heightCm, weeklyGoal = weekly)
            }
            bodyWeight?.let { app.bodyWeightRepo.log(userId, Units.toKg(it, unit)) }
            if (!onboarding) toast(R.string.text_profile_updated)
            finish()
        }
    }

    private fun finish() {
        if (onboarding) findNavController().navigate(R.id.action_onboarding_to_home)
        else findNavController().navigateUp()
    }

    companion object {
        const val ARG_ONBOARDING = "onboarding"
    }
}
