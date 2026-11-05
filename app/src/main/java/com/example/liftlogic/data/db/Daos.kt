package com.example.liftlogic.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Query("UPDATE users SET xp = xp + :amount WHERE userId = :userId")
    suspend fun addXp(userId: String, amount: Int)
}

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises WHERE ownerUserId IS NULL OR ownerUserId = :userId ORDER BY name COLLATE NOCASE")
    fun observeLibrary(userId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE exerciseId = :id")
    fun observeById(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE exerciseId = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE remoteId = :remoteId AND (ownerUserId IS NULL OR ownerUserId = :userId) LIMIT 1")
    suspend fun findByRemoteId(remoteId: Int, userId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE ownerUserId IS NULL")
    suspend fun getBuiltIn(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises WHERE ownerUserId IS NULL")
    suspend fun countBuiltIn(): Int

    @Insert
    suspend fun insert(exercise: ExerciseEntity): Long

    @Insert
    suspend fun insertAll(exercises: List<ExerciseEntity>): List<Long>

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Delete
    suspend fun delete(exercise: ExerciseEntity)
}

@Dao
abstract class RoutineDao {
    @Query(
        """SELECT r.*, (SELECT COUNT(*) FROM routine_exercises re WHERE re.routineId = r.routineId) AS exerciseCount
           FROM routines r WHERE r.ownerUserId = :userId OR r.ownerUserId IS NULL
           ORDER BY (r.ownerUserId IS NULL), r.name COLLATE NOCASE"""
    )
    abstract fun observeRoutines(userId: String): Flow<List<RoutineSummary>>

    @Query("SELECT * FROM routines WHERE routineId = :id")
    abstract fun observeRoutine(id: Long): Flow<RoutineEntity?>

    @Query("SELECT * FROM routines WHERE routineId = :id")
    abstract suspend fun getRoutine(id: Long): RoutineEntity?

    @Query(
        """SELECT re.*, e.name AS exerciseName, e.muscleGroup AS muscleGroup, e.equipment AS equipment
           FROM routine_exercises re JOIN exercises e ON e.exerciseId = re.exerciseId
           WHERE re.routineId = :routineId ORDER BY re.position"""
    )
    abstract fun observeRoutineExercises(routineId: Long): Flow<List<RoutineExerciseDetail>>

    @Query(
        """SELECT re.*, e.name AS exerciseName, e.muscleGroup AS muscleGroup, e.equipment AS equipment
           FROM routine_exercises re JOIN exercises e ON e.exerciseId = re.exerciseId
           WHERE re.routineId = :routineId ORDER BY re.position"""
    )
    abstract suspend fun getRoutineExercises(routineId: Long): List<RoutineExerciseDetail>

    @Query("SELECT COUNT(*) FROM routines WHERE ownerUserId IS NULL")
    abstract suspend fun countBuiltIn(): Int

    @Insert
    abstract suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    abstract suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routines WHERE routineId = :id")
    abstract suspend fun deleteRoutine(id: Long)

    @Insert
    abstract suspend fun insertItems(items: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercises WHERE routineId = :routineId")
    abstract suspend fun deleteItems(routineId: Long)

    /** Replaces all exercises of a routine in one transaction. */
    @Transaction
    open suspend fun replaceItems(routineId: Long, items: List<RoutineExerciseEntity>) {
        deleteItems(routineId)
        insertItems(items)
    }
}

@Dao
interface WorkoutDao {
    // ----- workouts -----
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Query("DELETE FROM workouts WHERE workoutId = :id")
    suspend fun deleteWorkout(id: Long)

    @Query("UPDATE workouts SET name = :name WHERE workoutId = :id")
    suspend fun renameWorkout(id: Long, name: String)

    @Query("SELECT * FROM workouts WHERE userId = :userId AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveWorkout(userId: String): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE userId = :userId AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveWorkout(userId: String): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE workoutId = :id")
    suspend fun getWorkout(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE workoutId = :id")
    fun observeWorkout(id: Long): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE userId = :userId AND finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun observeFinishedWorkouts(userId: String): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE userId = :userId AND finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    suspend fun getFinishedWorkouts(userId: String): List<WorkoutEntity>

    // ----- exercises inside a workout -----
    @Insert
    suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity): Long

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: Long)

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    fun observeWorkoutExercises(workoutId: Long): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY position")
    suspend fun getWorkoutExercises(workoutId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT MAX(position) FROM workout_exercises WHERE workoutId = :workoutId")
    suspend fun maxPosition(workoutId: Long): Int?

    @Query(
        """DELETE FROM workout_exercises WHERE workoutId = :workoutId
           AND id NOT IN (SELECT workoutExerciseId FROM workout_sets WHERE workoutId = :workoutId)"""
    )
    suspend fun deleteEmptyExercises(workoutId: Long)

    // ----- sets -----
    @Insert
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Insert
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Update
    suspend fun updateSet(set: WorkoutSetEntity)

    @Query("UPDATE workout_sets SET weightKg = :weightKg WHERE setId = :setId")
    suspend fun updateSetWeight(setId: Long, weightKg: Double?)

    @Query("UPDATE workout_sets SET reps = :reps WHERE setId = :setId")
    suspend fun updateSetReps(setId: Long, reps: Int?)

    @Query("UPDATE workout_sets SET weightKg = :weightKg, reps = :reps, completed = :completed WHERE setId = :setId")
    suspend fun updateSetValues(setId: Long, weightKg: Double?, reps: Int?, completed: Boolean)

    @Query("DELETE FROM workout_sets WHERE setId = :setId")
    suspend fun deleteSet(setId: Long)

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY workoutExerciseId, setNumber")
    fun observeSets(workoutId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY workoutExerciseId, setNumber")
    suspend fun getSets(workoutId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber")
    suspend fun getSetsForExercise(workoutExerciseId: Long): List<WorkoutSetEntity>

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND completed = 0")
    suspend fun deleteIncompleteSets(workoutId: Long)

    // ----- history across workouts -----
    @Query(
        """SELECT s.setId AS setId, s.workoutId AS workoutId, we.exerciseId AS exerciseId,
                  we.exerciseName AS exerciseName, s.setNumber AS setNumber, s.weightKg AS weightKg,
                  s.reps AS reps, w.finishedAt AS finishedAt
           FROM workout_sets s
           JOIN workout_exercises we ON we.id = s.workoutExerciseId
           JOIN workouts w ON w.workoutId = s.workoutId
           WHERE w.userId = :userId AND w.finishedAt IS NOT NULL AND s.completed = 1
                 AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL
           ORDER BY w.finishedAt DESC, s.setNumber"""
    )
    fun observeCompletedSets(userId: String): Flow<List<CompletedSet>>

    @Query(
        """SELECT s.setId AS setId, s.workoutId AS workoutId, we.exerciseId AS exerciseId,
                  we.exerciseName AS exerciseName, s.setNumber AS setNumber, s.weightKg AS weightKg,
                  s.reps AS reps, w.finishedAt AS finishedAt
           FROM workout_sets s
           JOIN workout_exercises we ON we.id = s.workoutExerciseId
           JOIN workouts w ON w.workoutId = s.workoutId
           WHERE w.userId = :userId AND w.finishedAt IS NOT NULL AND s.completed = 1
                 AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL
           ORDER BY w.finishedAt DESC, s.setNumber"""
    )
    suspend fun getCompletedSets(userId: String): List<CompletedSet>

    @Query(
        """SELECT s.setId AS setId, s.workoutId AS workoutId, we.exerciseId AS exerciseId,
                  we.exerciseName AS exerciseName, s.setNumber AS setNumber, s.weightKg AS weightKg,
                  s.reps AS reps, w.finishedAt AS finishedAt
           FROM workout_sets s
           JOIN workout_exercises we ON we.id = s.workoutExerciseId
           JOIN workouts w ON w.workoutId = s.workoutId
           WHERE w.userId = :userId AND we.exerciseId = :exerciseId AND w.finishedAt IS NOT NULL
                 AND s.completed = 1 AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL
           ORDER BY w.finishedAt DESC, s.setNumber"""
    )
    fun observeCompletedSetsForExercise(userId: String, exerciseId: Long): Flow<List<CompletedSet>>

    @Query(
        """SELECT s.setId AS setId, s.workoutId AS workoutId, we.exerciseId AS exerciseId,
                  we.exerciseName AS exerciseName, s.setNumber AS setNumber, s.weightKg AS weightKg,
                  s.reps AS reps, w.finishedAt AS finishedAt
           FROM workout_sets s
           JOIN workout_exercises we ON we.id = s.workoutExerciseId
           JOIN workouts w ON w.workoutId = s.workoutId
           WHERE w.userId = :userId AND we.exerciseId = :exerciseId AND w.finishedAt IS NOT NULL
                 AND s.completed = 1 AND s.weightKg IS NOT NULL AND s.reps IS NOT NULL
           ORDER BY w.finishedAt DESC, s.setNumber"""
    )
    suspend fun getCompletedSetsForExercise(userId: String, exerciseId: Long): List<CompletedSet>
}

@Dao
interface BodyWeightDao {
    @Insert
    suspend fun insert(entry: BodyWeightEntity): Long

    @Query("DELETE FROM body_weights WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM body_weights WHERE userId = :userId ORDER BY recordedAt ASC")
    fun observeHistory(userId: String): Flow<List<BodyWeightEntity>>
}

@Dao
interface BadgeDao {
    /** Returns -1 if the user already has the badge. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(badge: BadgeEntity): Long

    @Query("SELECT * FROM badges WHERE userId = :userId ORDER BY unlockedAt DESC")
    fun observeBadges(userId: String): Flow<List<BadgeEntity>>
}
