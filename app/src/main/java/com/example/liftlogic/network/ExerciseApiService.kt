package com.example.liftlogic.network

import retrofit2.http.GET
import retrofit2.http.Query

interface ExerciseApiService {

    @GET("api/v2/exercise/")
    suspend fun getExercises(
        @Query("category") categoryId: Int? = null,
        @Query("language") language: Int = 2,
        @Query("status") status: Int = 2,
        @Query("limit") limit: Int = 30,
        @Query("format") format: String = "json"
    ): ExerciseListResponse

    @GET("api/v2/exercise/search/")
    suspend fun searchExercises(
        @Query("term") term: String,
        @Query("language") language: String = "english",
        @Query("format") format: String = "json"
    ): ExerciseSearchResponse
}