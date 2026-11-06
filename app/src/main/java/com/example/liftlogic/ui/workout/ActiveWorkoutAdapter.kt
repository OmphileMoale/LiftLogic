package com.example.liftlogic.ui.workout

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.graphics.ColorUtils
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.databinding.ItemWorkoutAddSetBinding
import com.example.liftlogic.databinding.ItemWorkoutExerciseHeaderBinding
import com.example.liftlogic.databinding.ItemWorkoutSetBinding
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Units
import com.google.android.material.color.MaterialColors
import kotlin.math.abs

/** Everything the list needs to report back to the screen. */
interface WorkoutRowCallbacks {
    fun onWeightChanged(row: WorkoutRow.SetRow, text: String)
    fun onRepsChanged(row: WorkoutRow.SetRow, text: String)
    /** Return false to undo the tick (e.g. reps missing). */
    fun onSetCompleted(row: WorkoutRow.SetRow, completed: Boolean, weightText: String, repsText: String): Boolean
    fun onUsePrevious(row: WorkoutRow.SetRow)
    fun onAddSet(row: WorkoutRow.AddSet)
    fun onExerciseMenu(row: WorkoutRow.Header, anchor: android.view.View)
}

/**
 * Flat list of exercise headers, set rows and "Add set" buttons.
 * Text the user is typing is never overwritten when the same value comes back from the database.
 */
class ActiveWorkoutAdapter(
    private val callbacks: WorkoutRowCallbacks
) : ListAdapter<WorkoutRow, RecyclerView.ViewHolder>(DIFF) {

    inner class HeaderVH(val b: ItemWorkoutExerciseHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: WorkoutRow.Header) {
            val context = b.root.context
            b.name.text = row.exercise.exerciseName
            val rest = Formatters.clock(row.exercise.restSec)
            b.targetText.text = if (row.exercise.targetReps.isNotBlank()) {
                context.getString(R.string.format_target_and_rest, row.exercise.targetReps, rest)
            } else {
                context.getString(R.string.format_rest_only, rest)
            }
            b.weightLabel.text = row.unit.uppercase()
            b.moreButton.setOnClickListener { callbacks.onExerciseMenu(row, it) }
        }
    }

    inner class SetVH(val b: ItemWorkoutSetBinding) : RecyclerView.ViewHolder(b.root) {
        var current: WorkoutRow.SetRow? = null
        private var binding = false

        init {
            b.weightInput.doAfterTextChanged { text -> if (!binding) current?.let { callbacks.onWeightChanged(it, text?.toString().orEmpty()) } }
            b.repsInput.doAfterTextChanged { text -> if (!binding) current?.let { callbacks.onRepsChanged(it, text?.toString().orEmpty()) } }
            b.doneCheck.setOnClickListener {
                val row = current ?: return@setOnClickListener
                val checked = b.doneCheck.isChecked
                val ok = callbacks.onSetCompleted(row, checked, b.weightInput.text.toString(), b.repsInput.text.toString())
                if (!ok) b.doneCheck.isChecked = !checked
            }
            b.previous.setOnClickListener { current?.let { callbacks.onUsePrevious(it) } }
        }

        fun bind(row: WorkoutRow.SetRow) {
            binding = true
            current = row
            val context = b.root.context
            b.setNumber.text = row.set.setNumber.toString()

            val prev = row.previous
            b.previous.text = if (prev == null) "—" else
                context.getString(R.string.format_set_short, Units.number(Units.display(prev.weightKg, row.unit)), prev.reps)

            syncWeight(b.weightInput, row.set.weightKg?.let { Units.display(it, row.unit) })
            syncReps(b.repsInput, row.set.reps)
            b.weightInput.hint = prev?.let { Units.number(Units.display(it.weightKg, row.unit)) } ?: "0"
            b.repsInput.hint = prev?.reps?.toString() ?: row.targetReps.substringBefore('-').ifBlank { "0" }

            b.doneCheck.isChecked = row.set.completed
            val primary = MaterialColors.getColor(b.root, com.google.android.material.R.attr.colorPrimary)
            b.row.backgroundTintList = ColorStateList.valueOf(
                if (row.set.completed) ColorUtils.setAlphaComponent(primary, 45) else Color.TRANSPARENT
            )
            binding = false
        }

        /** Only replace the text if it represents a different number (keeps the cursor while typing). */
        private fun syncWeight(edit: EditText, value: Double?) {
            val shown = Units.parse(edit.text)
            val same = (shown == null && value == null) || (shown != null && value != null && abs(shown - value) < 0.01)
            if (!same) edit.setText(value?.let { Units.number(it) } ?: "")
        }

        private fun syncReps(edit: EditText, value: Int?) {
            val shown = edit.text.toString().trim().toIntOrNull()
            if (shown != value) edit.setText(value?.toString() ?: "")
        }
    }

    inner class AddSetVH(val b: ItemWorkoutAddSetBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(row: WorkoutRow.AddSet) {
            b.addSetButton.setOnClickListener { callbacks.onAddSet(row) }
        }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is WorkoutRow.Header -> TYPE_HEADER
        is WorkoutRow.SetRow -> TYPE_SET
        is WorkoutRow.AddSet -> TYPE_ADD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemWorkoutExerciseHeaderBinding.inflate(inflater, parent, false))
            TYPE_SET -> SetVH(ItemWorkoutSetBinding.inflate(inflater, parent, false))
            else -> AddSetVH(ItemWorkoutAddSetBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is WorkoutRow.Header -> (holder as HeaderVH).bind(row)
            is WorkoutRow.SetRow -> (holder as SetVH).bind(row)
            is WorkoutRow.AddSet -> (holder as AddSetVH).bind(row)
        }
    }

    fun rowAt(position: Int): WorkoutRow? = currentList.getOrNull(position)

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_SET = 1
        const val TYPE_ADD = 2

        private val DIFF = object : DiffUtil.ItemCallback<WorkoutRow>() {
            override fun areItemsTheSame(old: WorkoutRow, new: WorkoutRow) = old.id == new.id
            override fun areContentsTheSame(old: WorkoutRow, new: WorkoutRow) = old == new
        }
    }
}
