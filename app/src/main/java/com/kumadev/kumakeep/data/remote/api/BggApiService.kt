package com.kumadev.kumakeep.data.remote.api

import com.kumadev.kumakeep.data.remote.dto.BggHotResponse
import com.kumadev.kumakeep.data.remote.dto.BggSearchResponse
import com.kumadev.kumakeep.data.remote.dto.BggThingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface BggApiService {

    // NB: nessun filtro `type`. Con type=boardgame le espansioni passano ma
    // indistinguibili; senza filtro BGG restituisce ogni espansione due volte
    // (bucket boardgame + boardgameexpansion) → la dedup nel repository le
    // collassa e ricava il flag isExpansion dall'appartenenza al bucket.
    @GET("search")
    suspend fun search(
        @Query("query") query: String
    ): BggSearchResponse

    // NB: nessun filtro `type`. L'endpoint `thing` risolve per id; passare
    // type=boardgame escluderebbe le espansioni (items vuoto → "gioco non trovato").
    @GET("thing")
    suspend fun getGameDetail(
        @Query("id") id: Long,
        @Query("stats") stats: Int = 1
    ): BggThingResponse

    // Hot list ufficiale BGG, ordinata per rank. Endpoint dedicato e leggero:
    // nessun id in input, nessuna paginazione (BGG restituisce al massimo 50 item).
    @GET("hot")
    suspend fun getHotGames(
        @Query("type") type: String = "boardgame"
    ): BggHotResponse
}
