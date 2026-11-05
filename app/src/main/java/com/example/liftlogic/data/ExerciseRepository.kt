package com.example.liftlogic.data

import com.example.liftlogic.data.db.ExerciseDao
import com.example.liftlogic.data.db.ExerciseEntity
import com.example.liftlogic.network.ExerciseApiService
import com.example.liftlogic.network.ExerciseInfoDto

class ExerciseRepository(
    private val exerciseDao: ExerciseDao,
    private val api: ExerciseApiService
) {
    fun observeLibrary(userId: String) = exerciseDao.observeLibrary(userId)

    fun observeExercise(id: Long) = exerciseDao.observeById(id)

    suspend fun getExercise(id: Long) = exerciseDao.getById(id)

    suspend fun saveCustom(
        userId: String,
        existingId: Long?,
        name: String,
        muscle: MuscleGroup,
        equipment: Equipment,
        instructions: String
    ): Long {
        val existing = existingId?.let { exerciseDao.getById(it) }
        return if (existing != null) {
            exerciseDao.update(
                existing.copy(name = name.trim(), muscleGroup = muscle.key, equipment = equipment.key, instructions = instructions.trim())
            )
            existing.exerciseId
        } else {
            exerciseDao.insert(
                ExerciseEntity(
                    name = name.trim(),
                    muscleGroup = muscle.key,
                    equipment = equipment.key,
                    instructions = instructions.trim(),
                    isCustom = true,
                    ownerUserId = userId
                )
            )
        }
    }

    suspend fun delete(exercise: ExerciseEntity) = exerciseDao.delete(exercise)

    // ------------------------- wger.de REST API -------------------------

    /** Downloads exercises from wger.de. Only entries with an English name are returned. */
    suspend fun fetchOnline(categoryId: Int?): List<ExerciseInfoDto> =
        api.getExerciseInfo(categoryId = categoryId).results.orEmpty().filter { !it.englishName().isNullOrBlank() }

    /** Saves an online exercise into the user's library. Returns (id, wasAlreadyThere). */
    suspend fun importOnline(userId: String, dto: ExerciseInfoDto): Pair<Long, Boolean> {
        exerciseDao.findByRemoteId(dto.id, userId)?.let { return it.exerciseId to true }
        val id = exerciseDao.insert(
            ExerciseEntity(
                name = dto.englishName().orEmpty().trim(),
                muscleGroup = muscleForCategory(dto.category?.id).key,
                equipment = equipmentFor(dto).key,
                instructions = dto.cleanDescription(),
                isCustom = true,
                ownerUserId = userId,
                remoteId = dto.id
            )
        )
        return id to false
    }

    private fun muscleForCategory(categoryId: Int?): MuscleGroup = when (categoryId) {
        8 -> MuscleGroup.ARMS
        9, 14 -> MuscleGroup.LEGS
        10 -> MuscleGroup.CORE
        11 -> MuscleGroup.CHEST
        12 -> MuscleGroup.BACK
        13 -> MuscleGroup.SHOULDERS
        15 -> MuscleGroup.CARDIO
        else -> MuscleGroup.FULL_BODY
    }

    private fun equipmentFor(dto: ExerciseInfoDto): Equipment {
        val names = dto.equipment.orEmpty().mapNotNull { it.name?.lowercase() }
        return when {
            names.any { "barbell" in it || "sz-bar" in it } -> Equipment.BARBELL
            names.any { "dumbbell" in it } -> Equipment.DUMBBELL
            names.any { "kettlebell" in it } -> Equipment.KETTLEBELL
            names.any { "bodyweight" in it || "pull-up" in it } -> Equipment.BODYWEIGHT
            names.any { "band" in it } -> Equipment.BAND
            names.isEmpty() -> Equipment.BODYWEIGHT
            else -> Equipment.OTHER
        }
    }
}
