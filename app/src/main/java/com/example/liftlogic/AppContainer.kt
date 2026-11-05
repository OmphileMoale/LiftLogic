package com.example.liftlogic

import android.content.Context
import com.example.liftlogic.data.BodyWeightRepository
import com.example.liftlogic.data.DataSeeder
import com.example.liftlogic.data.ExerciseRepository
import com.example.liftlogic.data.RoutineRepository
import com.example.liftlogic.data.SessionManager
import com.example.liftlogic.data.UserRepository
import com.example.liftlogic.data.WorkoutRepository
import com.example.liftlogic.data.db.AppDatabase
import com.example.liftlogic.data.db.UserEntity
import com.example.liftlogic.network.RetrofitInstance
import com.example.liftlogic.notifications.NotificationHelper
import com.example.liftlogic.notifications.RestTimer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

/**
 * Simple manual dependency injection: one place that creates the database,
 * repositories and app-wide services. Created once in [LiftLogicApp].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppContainer(context: Context) {

    /** Lives as long as the app process (used for work that must outlive a screen). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val db: AppDatabase = AppDatabase.getInstance(context)
    val session = SessionManager(context)
    val notifications = NotificationHelper(context.applicationContext)

    val userRepo = UserRepository(db.userDao())
    val exerciseRepo = ExerciseRepository(db.exerciseDao(), RetrofitInstance.exerciseApi)
    val routineRepo = RoutineRepository(db)
    val workoutRepo = WorkoutRepository(db, appScope)
    val bodyWeightRepo = BodyWeightRepository(db.bodyWeightDao())
    val seeder = DataSeeder(db)

    /** The logged-in user (null when logged out). Updates automatically when the row changes. */
    val currentUser: StateFlow<UserEntity?> = session.userId
        .flatMapLatest { id -> if (id == null) flowOf(null) else userRepo.observeUser(id) }
        .stateIn(appScope, SharingStarted.Eagerly, null)

    val restTimer = RestTimer(appScope) {
        if (currentUser.value?.notificationsEnabled != false) notifications.restFinished()
    }

    /** Current weight unit ("kg" or "lb"). */
    val unit: String get() = currentUser.value?.units ?: "kg"
}
