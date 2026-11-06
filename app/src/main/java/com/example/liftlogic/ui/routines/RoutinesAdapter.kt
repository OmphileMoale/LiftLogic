package com.example.liftlogic.ui.routines

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.data.db.RoutineSummary
import com.example.liftlogic.databinding.ItemEmptyBinding
import com.example.liftlogic.databinding.ItemRoutineBinding
import com.example.liftlogic.databinding.ItemSectionHeaderBinding
import com.example.liftlogic.ui.visibleIf

/** Rows of the Workouts tab: section headers, routines and an empty-state message. */
sealed class RoutineRow {
    abstract val key: String

    data class Header(val title: Int) : RoutineRow() {
        override val key = "header_$title"
    }

    data class Item(val summary: RoutineSummary) : RoutineRow() {
        override val key = "routine_${summary.routine.routineId}"
    }

    data class Empty(val message: Int) : RoutineRow() {
        override val key = "empty_$message"
    }
}

class RoutinesAdapter(
    private val onClick: (RoutineSummary) -> Unit
) : ListAdapter<RoutineRow, RecyclerView.ViewHolder>(DIFF) {

    class HeaderVH(val binding: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    class ItemVH(val binding: ItemRoutineBinding) : RecyclerView.ViewHolder(binding.root)
    class EmptyVH(val binding: ItemEmptyBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is RoutineRow.Header -> TYPE_HEADER
        is RoutineRow.Item -> TYPE_ITEM
        is RoutineRow.Empty -> TYPE_EMPTY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderVH(ItemSectionHeaderBinding.inflate(inflater, parent, false))
            TYPE_EMPTY -> EmptyVH(ItemEmptyBinding.inflate(inflater, parent, false))
            else -> ItemVH(ItemRoutineBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is RoutineRow.Header -> (holder as HeaderVH).binding.title.setText(row.title)
            is RoutineRow.Empty -> (holder as EmptyVH).binding.text.setText(row.message)
            is RoutineRow.Item -> {
                val b = (holder as ItemVH).binding
                val summary = row.summary
                val context = b.root.context
                b.name.text = summary.routine.name
                b.programLabel.visibleIf(summary.routine.ownerUserId == null)
                val count = context.resources.getQuantityString(R.plurals.exercise_count, summary.exerciseCount, summary.exerciseCount)
                b.detail.text = if (summary.routine.description.isBlank()) count else "$count · ${summary.routine.description}"
                b.root.setOnClickListener { onClick(summary) }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val TYPE_EMPTY = 2

        private val DIFF = object : DiffUtil.ItemCallback<RoutineRow>() {
            override fun areItemsTheSame(old: RoutineRow, new: RoutineRow) = old.key == new.key
            override fun areContentsTheSame(old: RoutineRow, new: RoutineRow) = old == new
        }
    }
}
