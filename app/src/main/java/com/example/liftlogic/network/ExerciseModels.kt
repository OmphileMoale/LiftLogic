package com.example.liftlogic.network

import com.google.gson.annotations.SerializedName

data class ExerciseListResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<ExerciseDto>
)

data class ExerciseDto(
    val id: Int,
    val name: String?,
    val category: String?,
    val equipment: List<Int>? = null,
    val muscles: List<Int>? = null,
    val description: String? = null
) {
    fun cleanDescription(): String =
        (description ?: "").replace(Regex("<[^>]*>"), "").trim()
}

data class ExerciseSearchResponse(
    val suggestions: List<ExerciseSuggestion>? = null
)

data class ExerciseSuggestion(
    val value: String?,
    val data: ExerciseSuggestionData?
)

data class ExerciseSuggestionData(
    val id: Int,
    val name: String?,
    val category: String?,
    val image: String? = null
)