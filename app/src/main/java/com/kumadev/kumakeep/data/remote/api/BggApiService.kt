package com.kumadev.kumakeep.data.remote.api

import com.kumadev.kumakeep.data.remote.dto.BggSearchResponse
import com.kumadev.kumakeep.data.remote.dto.BggThingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BggApiService {

    @GET("search")
    suspend fun search(
        @Query("query") query: String,
        @Query("type") type: String = "boardgame"
    ): BggSearchResponse

    @GET("thing")
    suspend fun getGameDetail(
        @Query("id") id: Long,
        @Query("type") type: String = "boardgame",
        @Query("stats") stats: Int = 1
    ): BggThingResponse
}