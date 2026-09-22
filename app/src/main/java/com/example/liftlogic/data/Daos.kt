package com.example.liftlogic.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: UserEntity)

    @Update
    suspend fun update(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun findByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUser(userId: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun countByEmail(email: String): Int
}

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity)

    @Insert
    suspend fun insertSets(sets: List<ExerciseSetEntity>)

    @Query("SELECT * FROM workouts WHERE userId = :userId ORDER BY date DESC")
    fun observeWorkouts(userId: String): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM exercise_sets WHERE workoutId = :workoutId")
    suspend fun getSetsForWorkout(workoutId: String): List<ExerciseSetEntity>

    @Query("SELECT * FROM workouts WHERE synced = 0")
    suspend fun getUnsyncedWorkouts(): List<WorkoutEntity>

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)
}

@Dao
interface BodyMetricDao {
    @Insert
    suspend fun insert(metric: BodyMetricEntity)

    @Query("SELECT * FROM body_metrics WHERE userId = :userId ORDER BY recordedAt ASC")
    fun observeHistory(userId: String): Flow<List<BodyMetricEntity>>
}

@Dao
interface BadgeDao {
    @Insert
    suspend fun insert(badge: BadgeEntity)

    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun observeBadges(userId: String): Flow<List<BadgeEntity>>
}