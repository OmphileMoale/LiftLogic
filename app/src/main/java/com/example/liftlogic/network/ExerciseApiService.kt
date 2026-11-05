package com.example.liftlogic.network

import retrofit2.http.GET
import retrofit2.http.Query

/** wger.de public REST API (no key needed). Docs: https://wger.de/api/v2/ */
interface ExerciseApiService {

    @GET("api/v2/exerciseinfo/")
    suspend fun getExerciseInfo(
        @Query("category") categoryId: Int? = null,
        @Query("limit") limit: Int = 60,
        @Query("offset") offset: Int = 0,
        @Query("format") format: String = "json"
    ): ExerciseInfoListResponse
}
