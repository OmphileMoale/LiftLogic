package com.example.liftlogic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.liftlogic.R
import com.example.liftlogic.data.*
import com.example.liftlogic.network.ExerciseDto
import com.example.liftlogic.network.RetrofitInstance
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ActiveWorkoutSet(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Float,
    val reps: Int
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val authRepo = AuthRepository(db.userDao())
    private val workoutRepo = WorkoutRepository(db.workoutDao(), db.userDao(), db.badgeDao())

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUserId
        .flatMapLatest { id -> if (id == null) flowOf<UserEntity?>(null) else db.userDao().observeUser(id) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _authError = MutableStateFlow<AuthError?>(null)
    val authError: StateFlow<AuthError?> = _authError

    fun clearAuthError() { _authError.value = null }

    fun register(name: String, email: String, password: String, units: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = authRepo.register(name, email, password, units)) {
                // Account is created but NOT signed in automatically — the user must
                // log in with the credentials they just chose, on the Login screen.
                is AuthResult.Success -> onSuccess()
                is AuthResult.Failure -> _authError.value = result.error
            }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            when (val result = authRepo.login(email, password)) {
                is AuthResult.Success -> { _currentUserId.value = result.user.userId; onSuccess() }
                is AuthResult.Failure -> _authError.value = result.error
            }
        }
    }

    fun logout() { _currentUserId.value = null }

    fun updateSettings(units: String, notificationsEnabled: Boolean) {
        val id = _currentUserId.value ?: return
        viewModelScope.launch { authRepo.updateSettings(id, units, notificationsEnabled) }
    }

    private val _exercises = MutableStateFlow<List<ExerciseDto>>(emptyList())
    val exercises: StateFlow<List<ExerciseDto>> = _exercises

    private val _libraryLoading = MutableStateFlow(false)
    val libraryLoading: StateFlow<Boolean> = _libraryLoading

    private val _libraryError = MutableStateFlow<String?>(null)
    val libraryError: StateFlow<String?> = _libraryError

    fun loadExercises(categoryId: Int? = null) {
        viewModelScope.launch {
            _libraryLoading.value = true
            _libraryError.value = null
            try {
                val response = RetrofitInstance.exerciseApi.getExercises(categoryId = categoryId)
                _exercises.value = response.results.filter { !it.name.isNullOrBlank() }
            } catch (e: Exception) {
                val app = getApplication<Application>()
                val detail = e.localizedMessage ?: app.getString(R.string.error_network_generic)
                _libraryError.value = app.getString(R.string.error_library_load, detail)
            } finally {
                _libraryLoading.value = false
            }
        }
    }

    private val _activeSets = MutableStateFlow<List<ActiveWorkoutSet>>(emptyList())
    val activeSets: StateFlow<List<ActiveWorkoutSet>> = _activeSets

    fun addSet(exerciseId: String, exerciseName: String, weightKg: Float, reps: Int) {
        _activeSets.value = _activeSets.value + ActiveWorkoutSet(exerciseId, exerciseName, weightKg, reps)
    }

    fun clearActiveWorkout() { _activeSets.value = emptyList() }

    private val _lastResult = MutableStateFlow<FinishWorkoutResult?>(null)
    val lastResult: StateFlow<FinishWorkoutResult?> = _lastResult

    fun finishWorkout(durationSec: Int, onDone: () -> Unit) {
        val userId = _currentUserId.value ?: return
        viewModelScope.launch {
            val sets = _activeSets.value.map {
                LoggedSetInput(it.exerciseId, it.exerciseName, it.weightKg, it.reps)
            }
            if (sets.isEmpty()) return@launch
            val result = workoutRepo.finishWorkout(userId, durationSec, sets)
            _lastResult.value = result
            clearActiveWorkout()
            onDone()
        }
    }

    fun workoutHistory(): Flow<List<WorkoutEntity>> {
        val userId = _currentUserId.value ?: return flowOf(emptyList())
        return db.workoutDao().observeWorkouts(userId)
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(application) as T
            }
    }
}