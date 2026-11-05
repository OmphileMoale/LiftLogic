package com.example.liftlogic.network

data class ExerciseInfoListResponse(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<ExerciseInfoDto>? = null
)

data class ExerciseInfoDto(
    val id: Int,
    val category: NamedDto? = null,
    val equipment: List<NamedDto>? = null,
    val muscles: List<NamedDto>? = null,
    /** Current API field name for the per-language names/descriptions */
    val translations: List<TranslationDto>? = null,
    /** Older API versions used this field name instead */
    val exercises: List<TranslationDto>? = null
) {
    private fun english(): TranslationDto? =
        (translations ?: exercises).orEmpty().firstOrNull { it.language == ENGLISH }

    fun englishName(): String? = english()?.name

    fun cleanDescription(): String =
        english()?.description.orEmpty()
            .replace(Regex("<[^>]*>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private companion object {
        const val ENGLISH = 2
    }
}

data class NamedDto(
    val id: Int,
    val name: String? = null
)

data class TranslationDto(
    val id: Int? = null,
    val name: String? = null,
    val description: String? = null,
    val language: Int? = null
)
