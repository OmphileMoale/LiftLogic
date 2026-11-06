package com.example.liftlogic.ui.exercises

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.liftlogic.R
import com.example.liftlogic.data.MuscleGroup
import com.example.liftlogic.databinding.FragmentExerciseListBinding
import com.example.liftlogic.databinding.ItemFilterChipBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.appViewModels
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.visibleIf

/**
 * The exercise library (Exercises tab). With pickMode = true the same screen is used to
 * choose exercises for a routine or a workout; the chosen ids are returned to the previous screen.
 */
class ExerciseListFragment : BaseFragment<FragmentExerciseListBinding>(FragmentExerciseListBinding::inflate) {

    private val pickMode: Boolean get() = arguments?.getBoolean(ARG_PICK_MODE) ?: false

    private val viewModel: ExerciseListViewModel by appViewModels { ExerciseListViewModel(it) }

    private lateinit var adapter: ExerciseAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.appBar.toolbar

        if (pickMode) {
            setupToolbar(toolbar, R.string.title_add_exercises)
            binding.addCustomFab.visibleIf(false)
            binding.addSelectedButton.visibleIf(true)
        } else {
            toolbar.setTitle(R.string.tab_exercises)
            toolbar.navigationIcon = null
            toolbar.inflateMenu(R.menu.menu_exercise_library)
            toolbar.setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_browse_online) {
                    findNavController().navigate(R.id.onlineExercisesFragment)
                    true
                } else false
            }
            binding.addCustomFab.setOnClickListener { findNavController().navigate(R.id.exerciseEditorFragment) }
        }

        adapter = ExerciseAdapter(pickMode) { exercise ->
            if (pickMode) {
                viewModel.toggle(exercise.exerciseId)
            } else {
                findNavController().navigate(
                    R.id.exerciseDetailFragment,
                    bundleOf(ExerciseDetailFragment.ARG_EXERCISE_ID to exercise.exerciseId)
                )
            }
        }
        binding.exerciseList.layoutManager = LinearLayoutManager(requireContext())
        binding.exerciseList.adapter = adapter

        setupMuscleChips()
        binding.searchInput.doAfterTextChanged { viewModel.setQuery(it?.toString().orEmpty()) }

        observe(viewModel.exercises) { list ->
            adapter.submitList(list)
            binding.emptyText.visibleIf(list.isEmpty())
        }

        observe(viewModel.selected) { ids ->
            adapter.selected = ids.toSet()
            binding.addSelectedButton.isEnabled = ids.isNotEmpty()
            binding.addSelectedButton.text = if (ids.isEmpty()) {
                getString(R.string.action_select_exercises)
            } else {
                resources.getQuantityString(R.plurals.action_add_n_exercises, ids.size, ids.size)
            }
        }

        binding.addSelectedButton.setOnClickListener {
            val ids = viewModel.selected.value
            if (ids.isEmpty()) return@setOnClickListener
            findNavController().previousBackStackEntry?.savedStateHandle?.set(RESULT_PICKED_IDS, ids.toLongArray())
            findNavController().popBackStack()
        }
    }

    private fun setupMuscleChips() {
        val group = binding.muscleGroup
        group.removeAllViews()
        val options: List<MuscleGroup?> = listOf<MuscleGroup?>(null) + MuscleGroup.entries
        options.forEachIndexed { index, muscle ->
            val chip = ItemFilterChipBinding.inflate(layoutInflater, group, false).root
            chip.id = View.generateViewId()
            chip.setText(muscle?.label ?: R.string.category_all)
            chip.isChecked = viewModel.muscle.value == muscle
            chip.setOnCheckedChangeListener { _, checked -> if (checked) viewModel.setMuscle(muscle) }
            group.addView(chip)
            if (index == 0 && viewModel.muscle.value == null) chip.isChecked = true
        }
    }

    override fun onDestroyView() {
        binding.exerciseList.adapter = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_PICK_MODE = "pickMode"
        const val RESULT_PICKED_IDS = "picked_exercise_ids"
    }
}
