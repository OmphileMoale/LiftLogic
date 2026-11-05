package com.example.liftlogic.ui.exercises

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.data.Equipment
import com.example.liftlogic.data.MuscleGroup
import com.example.liftlogic.data.db.ExerciseEntity
import com.example.liftlogic.databinding.ItemExerciseBinding
import com.example.liftlogic.ui.visibleIf

/** Exercise rows for the library and the picker (with a checkbox in picker mode). */
class ExerciseAdapter(
    private val pickMode: Boolean,
    private val onClick: (ExerciseEntity) -> Unit
) : ListAdapter<ExerciseEntity, ExerciseAdapter.ViewHolder>(DIFF) {

    var selected: Set<Long> = emptySet()
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    class ViewHolder(val binding: ItemExerciseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ex = getItem(position)
        val b = holder.binding
        val context = b.root.context
        b.initial.text = ex.name.firstOrNull()?.uppercase() ?: "?"
        b.name.text = ex.name
        b.detail.text = context.getString(
            R.string.format_two_parts,
            context.getString(MuscleGroup.fromKey(ex.muscleGroup).label),
            context.getString(Equipment.fromKey(ex.equipment).label)
        )
        b.customLabel.visibleIf(ex.isCustom)
        b.check.visibleIf(pickMode)
        b.chevron.visibleIf(!pickMode)
        b.check.isChecked = ex.exerciseId in selected
        b.root.setOnClickListener { onClick(ex) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ExerciseEntity>() {
            override fun areItemsTheSame(old: ExerciseEntity, new: ExerciseEntity) = old.exerciseId == new.exerciseId
            override fun areContentsTheSame(old: ExerciseEntity, new: ExerciseEntity) = old == new
        }
    }
}
