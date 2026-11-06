package com.example.liftlogic.ui.exercises

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.navigation.fragment.findNavController
import com.example.liftlogic.R
import com.example.liftlogic.data.Equipment
import com.example.liftlogic.data.MuscleGroup
import com.example.liftlogic.databinding.FragmentExerciseEditorBinding
import com.example.liftlogic.ui.BaseFragment

/** Create or edit a custom exercise. */
class ExerciseEditorFragment : BaseFragment<FragmentExerciseEditorBinding>(FragmentExerciseEditorBinding::inflate) {

    private val exerciseId: Long? get() = arguments?.getLong(ARG_EXERCISE_ID, -1L)?.takeIf { it > 0 }

    private var muscle = MuscleGroup.CHEST
    private var equipment = Equipment.BARBELL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.appBar.toolbar, if (exerciseId == null) R.string.title_custom_exercise else R.string.title_edit_exercise)

        savedInstanceState?.let {
            muscle = MuscleGroup.fromKey(it.getString(STATE_MUSCLE))
            equipment = Equipment.fromKey(it.getString(STATE_EQUIPMENT))
        }

        val muscles = MuscleGroup.entries
        val equipments = Equipment.entries
        binding.muscleDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, muscles.map { getString(it.label) }))
        binding.equipmentDropdown.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, equipments.map { getString(it.label) }))
        binding.muscleDropdown.setOnItemClickListener { _, _, position, _ -> muscle = muscles[position] }
        binding.equipmentDropdown.setOnItemClickListener { _, _, position, _ -> equipment = equipments[position] }
        showSelections()

        val id = exerciseId
        if (id != null && savedInstanceState == null) {
            launchUi {
                val ex = app.exerciseRepo.getExercise(id) ?: return@launchUi
                val b = bindingOrNull ?: return@launchUi
                b.nameInput.setText(ex.name)
                b.instructionsInput.setText(ex.instructions)
                muscle = MuscleGroup.fromKey(ex.muscleGroup)
                equipment = Equipment.fromKey(ex.equipment)
                showSelections()
            }
        }

        binding.saveButton.setOnClickListener { save() }
    }

    private fun showSelections() {
        // false = don't filter the dropdown list by the current text
        binding.muscleDropdown.setText(getString(muscle.label), false)
        binding.equipmentDropdown.setText(getString(equipment.label), false)
    }

    private fun save() {
        val userId = app.session.currentUserId ?: return
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()
        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.error_exercise_name)
            return
        }
        binding.nameLayout.error = null
        val instructions = binding.instructionsInput.text?.toString().orEmpty()
        launchUi {
            app.exerciseRepo.saveCustom(userId, exerciseId, name, muscle, equipment, instructions)
            toast(R.string.text_exercise_saved)
            findNavController().navigateUp()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_MUSCLE, muscle.key)
        outState.putString(STATE_EQUIPMENT, equipment.key)
    }

    companion object {
        const val ARG_EXERCISE_ID = "exerciseId"
        private const val STATE_MUSCLE = "muscle"
        private const val STATE_EQUIPMENT = "equipment"
    }
}
