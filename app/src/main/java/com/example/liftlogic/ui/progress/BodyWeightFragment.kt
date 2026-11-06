package com.example.liftlogic.ui.progress

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.R
import com.example.liftlogic.data.db.BodyWeightEntity
import com.example.liftlogic.databinding.FragmentBodyWeightBinding
import com.example.liftlogic.databinding.ItemBodyWeightBinding
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.showTextInputDialog
import com.example.liftlogic.ui.visibleIf
import com.example.liftlogic.util.Formatters
import com.example.liftlogic.util.Units
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest

/** Body-weight log with a chart. Swipe an entry to delete it. */
class BodyWeightFragment : BaseFragment<FragmentBodyWeightBinding>(FragmentBodyWeightBinding::inflate) {

    private val adapter = EntryAdapter()
    private var unit = Units.KG

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.appBar.toolbar, R.string.title_body_weight)

        binding.entryList.layoutManager = LinearLayoutManager(requireContext())
        binding.entryList.adapter = adapter
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, a: RecyclerView.ViewHolder, b: RecyclerView.ViewHolder) = false
            override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) {
                val entry = adapter.currentList.getOrNull(holder.bindingAdapterPosition) ?: return
                launchUi { app.bodyWeightRepo.delete(entry.entry.id) }
            }
        }).attachToRecyclerView(binding.entryList)

        binding.addFab.setOnClickListener { askWeight() }

        val history = app.session.userId.flatMapLatest { id ->
            if (id == null) emptyFlow() else app.bodyWeightRepo.observeHistory(id)
        }
        observe(combine(history, app.currentUser) { list, user -> list to (user?.units ?: Units.KG) }) { (list, u) ->
            unit = u
            binding.chart.visibleIf(list.size >= 2)
            binding.chartEmpty.visibleIf(list.size < 2)
            binding.chart.setData(
                list.map { Units.fromKg(it.weightKg, u).toFloat() },
                list.map { Formatters.shortDate(it.recordedAt) }
            ) { Units.number(it.toDouble()) }

            // Newest first, with the change since the entry before it
            val rows = list.mapIndexed { index, entry ->
                val before = list.getOrNull(index - 1)
                Entry(entry, before?.let { entry.weightKg - it.weightKg }, u)
            }.reversed()
            adapter.submitList(rows)
        }
    }

    private fun askWeight() {
        val userId = app.session.currentUserId ?: return
        showTextInputDialog(R.string.action_log_weight, R.string.label_weight, numeric = true) { text ->
            val value = Units.parse(text)
            if (value != null && value > 0) {
                launchUi { app.bodyWeightRepo.log(userId, Units.toKg(value, unit)) }
            }
        }
    }

    override fun onDestroyView() {
        binding.entryList.adapter = null
        super.onDestroyView()
    }

    private data class Entry(val entry: BodyWeightEntity, val changeKg: Double?, val unit: String)

    private class EntryAdapter : ListAdapter<Entry, EntryAdapter.VH>(DIFF) {
        class VH(val b: ItemBodyWeightBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemBodyWeightBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = getItem(position)
            holder.b.date.text = Formatters.date(row.entry.recordedAt)
            holder.b.weight.text = Units.weight(row.entry.weightKg, row.unit)
            val change = row.changeKg?.let { Units.fromKg(it, row.unit) }
            holder.b.change.text = when {
                change == null -> ""
                change > 0 -> "+${Units.number(change)}"
                else -> Units.number(change)
            }
        }

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<Entry>() {
                override fun areItemsTheSame(old: Entry, new: Entry) = old.entry.id == new.entry.id
                override fun areContentsTheSame(old: Entry, new: Entry) = old == new
            }
        }
    }
}
