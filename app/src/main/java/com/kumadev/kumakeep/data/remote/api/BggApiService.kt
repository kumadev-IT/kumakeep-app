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

    // NB: nessun filtro `type`. L'endpoint `thing` risolve per id; passare
    // type=boardgame escluderebbe le espansioni (items vuoto → "gioco non trovato").
    @GET("thing")
    suspend fun getGameDetail(
        @Query("id") id: Long,
        @Query("stats") stats: Int = 1
    ): BggThingResponse
}