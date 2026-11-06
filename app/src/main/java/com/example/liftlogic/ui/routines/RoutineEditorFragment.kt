package com.example.liftlogic.ui.routines

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.data.MuscleGroup
import com.example.liftlogic.data.RoutineItemDraft
import com.example.liftlogic.databinding.FragmentRoutineEditorBinding
import com.example.liftlogic.databinding.ItemRoutineEditorBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.appViewModels
import com.example.liftlogic.ui.exercises.ExerciseListFragment
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.visibleIf

/** Create a new routine or edit one of the user's routines. */
class RoutineEditorFragment : BaseFragment<FragmentRoutineEditorBinding>(FragmentRoutineEditorBinding::inflate) {

    private val routineId: Long? get() = arguments?.getLong(ARG_ROUTINE_ID, -1L)?.takeIf { it > 0 }

    private val viewModel: RoutineEditorViewModel by appViewModels { RoutineEditorViewModel(it, routineId) }

    private lateinit var adapter: EditorAdapter
    private lateinit var touchHelper: ItemTouchHelper

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.appBar.toolbar, if (routineId == null) R.string.title_new_routine else R.string.title_edit_routine)

        adapter = EditorAdapter(viewModel.items, onRemove = { position -> viewModel.remove(position) }, onStartDrag = { touchHelper.startDrag(it) })
        binding.itemList.layoutManager = LinearLayoutManager(requireContext())
        binding.itemList.adapter = adapter

        touchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
                viewModel.move(from.bindingAdapterPosition, to.bindingAdapterPosition)
                adapter.notifyItemMoved(from.bindingAdapterPosition, to.bindingAdapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit
            override fun isLongPressDragEnabled() = true
        })
        touchHelper.attachToRecyclerView(binding.itemList)

        // Name / description
        if (savedInstanceState == null && routineId == null) {
            binding.nameInput.setText(viewModel.name)
        }
        binding.nameInput.doAfterTextChanged { viewModel.name = it?.toString().orEmpty() }
        binding.descriptionInput.doAfterTextChanged { viewModel.description = it?.toString().orEmpty() }

        observe(viewModel.loaded) { loaded ->
            if (loaded != null && binding.nameInput.text.isNullOrEmpty() && savedInstanceState == null) {
                binding.nameInput.setText(loaded.first)
                binding.descriptionInput.setText(loaded.second)
            }
        }

        observe(viewModel.version) {
            adapter.notifyDataSetChanged()
            binding.emptyText.visibleIf(viewModel.items.isEmpty())
            binding.hintText.visibleIf(viewModel.items.size > 1)
        }

        // Exercises chosen in the picker come back through the back-stack entry's SavedStateHandle
        val handle = findNavController().currentBackStackEntry?.savedStateHandle
        if (handle != null) {
            observe(handle.getStateFlow<LongArray?>(ExerciseListFragment.RESULT_PICKED_IDS, null)) { ids ->
                if (ids != null) {
                    viewModel.addExercises(ids)
                    handle.remove<LongArray>(ExerciseListFragment.RESULT_PICKED_IDS)
                }
            }
        }

        binding.addExercisesButton.setOnClickListener { findNavController().navigate(R.id.exercisePickerFragment) }
        binding.saveButton.setOnClickListener { save() }
    }

    private fun save() {
        val name = binding.nameInput.text?.toString().orEmpty()
        if (name.isBlank()) {
            binding.nameLayout.error = getString(R.string.error_routine_name)
            return
        }
        binding.nameLayout.error = null
        if (viewModel.items.isEmpty()) {
            toast(R.string.error_routine_empty)
            return
        }
        binding.saveButton.isEnabled = false
        launchUi {
            val id = viewModel.save()
            if (id != null) {
                toast(R.string.text_routine_saved)
                findNavController().navigateUp()
            } else {
                bindingOrNull?.saveButton?.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        binding.itemList.adapter = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ROUTINE_ID = "routineId"
    }

    /** Editable rows. Text changes are written straight into the draft objects. */
    @SuppressLint("ClickableViewAccessibility")
    private class EditorAdapter(
        private val items: MutableList<RoutineItemDraft>,
        private val onRemove: (Int) -> Unit,
        private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
    ) : RecyclerView.Adapter<EditorAdapter.VH>() {

        inner class VH(val b: ItemRoutineEditorBinding) : RecyclerView.ViewHolder(b.root) {
            private var binding = false
            private val current: RoutineItemDraft? get() = items.getOrNull(bindingAdapterPosition)

            init {
                b.setsInput.doAfterTextChanged { if (!binding) current?.targetSets = it?.toString()?.toIntOrNull() ?: 1 }
                b.repsInput.doAfterTextChanged { if (!binding) current?.targetReps = it?.toString().orEmpty() }
                b.restInput.doAfterTextChanged { if (!binding) current?.restSec = it?.toString()?.toIntOrNull() ?: 0 }
                b.removeButton.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onRemove(pos)
                }
                b.dragHandle.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) onStartDrag(this)
                    false
                }
            }

            fun bind(item: RoutineItemDraft) {
                binding = true
                b.name.text = item.exerciseName
                b.muscle.setText(MuscleGroup.fromKey(item.muscleGroup).label)
                b.setsInput.setText(item.targetSets.toString())
                b.repsInput.setText(item.targetReps)
                b.restInput.setText(item.restSec.toString())
                binding = false
            }
        }

        override fun getItemCount() = items.size

        override fun getItemId(position: Int) = items[position].key

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemRoutineEditorBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])
    }
}
