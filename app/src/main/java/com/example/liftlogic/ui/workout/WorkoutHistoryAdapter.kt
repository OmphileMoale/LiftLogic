package com.example.liftlogic.ui.workout

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.data.db.WorkoutEntity
import com.example.liftlogic.databinding.ItemWorkoutHistoryBinding
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Units
import com.google.android.material.color.MaterialColors

/** List of finished workouts (Home and History). */
class WorkoutHistoryAdapter(
    private val onClick: (WorkoutEntity) -> Unit
) : ListAdapter<WorkoutEntity, WorkoutHistoryAdapter.ViewHolder>(DIFF) {

    var unit: String = Units.KG
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    class ViewHolder(val binding: ItemWorkoutHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemWorkoutHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val w = getItem(position)
        val b = holder.binding
        val context = b.root.context
        b.name.text = w.name
        b.date.text = Formatters.dateTime(w.finishedAt ?: w.startedAt)
        b.stats.text = context.getString(
            R.string.format_workout_stats,
            Formatters.duration(w.durationSec),
            Units.volume(w.totalVolumeKg, unit),
            context.resources.getQuantityString(R.plurals.set_count, w.totalSets, w.totalSets)
        )
        b.prBadge.visibleIf(w.prCount > 0)
        b.prBadge.text = w.prCount.toString()

        val primary = MaterialColors.getColor(b.root, com.google.android.material.R.attr.colorPrimary)
        b.iconBackground.backgroundTintList = ColorStateList.valueOf(ColorUtils.setAlphaComponent(primary, 30))
        b.root.setOnClickListener { onClick(w) }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<WorkoutEntity>() {
            override fun areItemsTheSame(old: WorkoutEntity, new: WorkoutEntity) = old.workoutId == new.workoutId
            override fun areContentsTheSame(old: WorkoutEntity, new: WorkoutEntity) = old == new
        }
    }
}
