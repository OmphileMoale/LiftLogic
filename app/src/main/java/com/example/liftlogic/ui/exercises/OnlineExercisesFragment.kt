package com.example.liftlogic.ui.exercises

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.liftlogic.AppContainer
import com.example.liftlogic.R
import com.example.liftlogic.databinding.FragmentOnlineExercisesBinding
import com.example.liftlogic.databinding.ItemOnlineExerciseBinding
import com.example.liftlogic.network.ExerciseInfoDto
import com.example.liftlogic.ui.BaseFragment
import com.example.liftlogic.ui.appViewModels
import com.example.liftlogic.ui.observe
import com.example.liftlogic.ui.visibleIf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OnlineState(
    val loading: Boolean = false,
    val error: String? = null,
    val results: List<ExerciseInfoDto> = emptyList()
)

/** Loads exercises from the wger.de REST API. */
class OnlineExercisesViewModel(private val app: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(OnlineState())
    val state: StateFlow<OnlineState> = _state.asStateFlow()

    var categoryId: Int? = null
        private set
    private var loadedOnce = false

    fun loadIfNeeded() {
        if (!loadedOnce) load(categoryId)
    }

    fun load(category: Int?) {
        categoryId = category
        loadedOnce = true
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            _state.value = try {
                OnlineState(results = app.exerciseRepo.fetchOnline(category))
            } catch (e: Exception) {
                OnlineState(error = e.localizedMessage ?: "network error")
            }
        }
    }

    /** Returns true if it was newly added, false if it was already in the library. */
    suspend fun addToLibrary(dto: ExerciseInfoDto): Boolean {
        val userId = app.session.currentUserId ?: return false
        return !app.exerciseRepo.importOnline(userId, dto).second
    }
}

class OnlineExercisesFragment : BaseFragment<FragmentOnlineExercisesBinding>(FragmentOnlineExercisesBinding::inflate) {

    private val viewModel: OnlineExercisesViewModel by appViewModels { OnlineExercisesViewModel(it) }

    /** Chip id -> wger.de category id (null = all) */
    private val categories = mapOf(
        R.id.chip_all to null,
        R.id.chip_chest to 11,
        R.id.chip_back to 12,
        R.id.chip_legs to 9,
        R.id.chip_shoulders to 13,
        R.id.chip_arms to 8,
        R.id.chip_abs to 10,
        R.id.chip_cardio to 15
    )

    private val adapter = OnlineAdapter { dto ->
        launchUi {
            val added = viewModel.addToLibrary(dto)
            toast(if (added) getString(R.string.format_added_to_library, dto.englishName()) else getString(R.string.text_already_in_library))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar(binding.appBar.toolbar, R.string.title_browse_online)

        binding.resultList.layoutManager = LinearLayoutManager(requireContext())
        binding.resultList.adapter = adapter

        val selected = categories.entries.firstOrNull { it.value == viewModel.categoryId }?.key ?: R.id.chip_all
        binding.categoryGroup.check(selected)
        binding.categoryGroup.setOnCheckedStateChangeListener { _, ids ->
            val id = ids.firstOrNull() ?: return@setOnCheckedStateChangeListener
            viewModel.load(categories[id])
        }
        binding.retryButton.setOnClickListener { viewModel.load(viewModel.categoryId) }

        viewModel.loadIfNeeded()

        observe(viewModel.state) { state ->
            binding.loading.visibleIf(state.loading)
            binding.errorBox.visibleIf(!state.loading && state.error != null)
            binding.errorText.text = state.error?.let { getString(R.string.error_library_load, it) }
            binding.resultList.visibleIf(!state.loading && state.error == null)
            adapter.submitList(state.results)
        }
    }

    override fun onDestroyView() {
        binding.resultList.adapter = null
        super.onDestroyView()
    }

    private class OnlineAdapter(private val onAdd: (ExerciseInfoDto) -> Unit) :
        ListAdapter<ExerciseInfoDto, OnlineAdapter.VH>(DIFF) {

        class VH(val b: ItemOnlineExerciseBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(ItemOnlineExerciseBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val dto = getItem(position)
            holder.b.name.text = dto.englishName()
            holder.b.category.text = dto.category?.name.orEmpty()
            val description = dto.cleanDescription()
            holder.b.description.text = description
            holder.b.description.visibleIf(description.isNotBlank())
            holder.b.addButton.setOnClickListener { onAdd(dto) }
            holder.b.root.setOnClickListener { onAdd(dto) }
        }

        companion object {
            private val DIFF = object : DiffUtil.ItemCallback<ExerciseInfoDto>() {
                override fun areItemsTheSame(old: ExerciseInfoDto, new: ExerciseInfoDto) = old.id == new.id
                override fun areContentsTheSame(old: ExerciseInfoDto, new: ExerciseInfoDto) = old == new
            }
        }
    }
}
