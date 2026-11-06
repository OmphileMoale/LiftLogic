package com.example.liftlogic.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.liftlogic.R

/** Muscle groups used to classify exercises. The key is what is stored in the database. */
enum class MuscleGroup(val key: String, @StringRes val label: Int) {
    CHEST("chest", R.string.category_chest),
    BACK("back", R.string.category_back),
    LEGS("legs", R.string.category_legs),
    SHOULDERS("shoulders", R.string.category_shoulders),
    ARMS("arms", R.string.category_arms),
    CORE("core", R.string.muscle_core),
    FULL_BODY("full_body", R.string.muscle_full_body),
    CARDIO("cardio", R.string.muscle_cardio);

    companion object {
        fun fromKey(key: String?): MuscleGroup = entries.firstOrNull { it.key == key } ?: FULL_BODY
    }
}

enum class Equipment(val key: String, @StringRes val label: Int) {
    BARBELL("barbell", R.string.equipment_barbell),
    DUMBBELL("dumbbell", R.string.equipment_dumbbell),
    MACHINE("machine", R.string.equipment_machine),
    CABLE("cable", R.string.equipment_cable),
    BODYWEIGHT("bodyweight", R.string.equipment_bodyweight),
    KETTLEBELL("kettlebell", R.string.equipment_kettlebell),
    BAND("band", R.string.equipment_band),
    OTHER("other", R.string.equipment_other);

    companion object {
        fun fromKey(key: String?): Equipment = entries.firstOrNull { it.key == key } ?: OTHER
    }
}

enum class FitnessGoal(val key: String, @StringRes val label: Int) {
    BUILD_MUSCLE("build_muscle", R.string.goal_build_muscle),
    GET_STRONGER("get_stronger", R.string.goal_get_stronger),
    LOSE_FAT("lose_fat", R.string.goal_lose_fat),
    STAY_FIT("stay_fit", R.string.goal_stay_fit);

    companion object {
        fun fromKey(key: String?): FitnessGoal = entries.firstOrNull { it.key == key } ?: BUILD_MUSCLE
    }
}

enum class Experience(val key: String, @StringRes val label: Int) {
    BEGINNER("beginner", R.string.experience_beginner),
    INTERMEDIATE("intermediate", R.string.experience_intermediate),
    ADVANCED("advanced", R.string.experience_advanced);

    companion object {
        fun fromKey(key: String?): Experience = entries.firstOrNull { it.key == key } ?: BEGINNER
    }
}

/** Achievements that can be unlocked by finishing workouts. */
enum class Badge(
    val key: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int
) {
    FIRST_WORKOUT("first_workout", R.string.badge_first_workout, R.string.badge_first_workout_desc, R.drawable.ic_fitness_center),
    WORKOUTS_10("workouts_10", R.string.badge_workouts_10, R.string.badge_workouts_10_desc, R.drawable.ic_star),
    WORKOUTS_50("workouts_50", R.string.badge_workouts_50, R.string.badge_workouts_50_desc, R.drawable.ic_emoji_events),
    STREAK_3("streak_3", R.string.badge_streak_3, R.string.badge_streak_3_desc, R.drawable.ic_whatshot),
    STREAK_7("streak_7", R.string.badge_streak_7, R.string.badge_streak_7_desc, R.drawable.ic_whatshot),
    VOLUME_10K("volume_10k", R.string.badge_volume_10k, R.string.badge_volume_10k_desc, R.drawable.ic_trending_up),
    FIRST_PR("first_pr", R.string.badge_first_pr, R.string.badge_first_pr_desc, R.drawable.ic_auto_awesome),
    EARLY_BIRD("early_bird", R.string.badge_early_bird, R.string.badge_early_bird_desc, R.drawable.ic_flash_on);

    companion object {
        fun fromKey(key: String): Badge? = entries.firstOrNull { it.key == key }
    }
}
