package com.kumadev.kumakeep.domain.repository

import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface BoardGameRepository {
    suspend fun searchBgg(query: String): Result<List<SearchResult>>
    suspend fun getGameDetail(bggId: Long): Result<BoardGame>
    fun getLibraryGames(): Flow<List<BoardGame>>
    fun getPlayedCount(): Flow<Int>
    fun getWishlistGameCount(): Flow<Int>
    fun getRecentlyViewedGames(bggIds: List<Long>): Flow<List<BoardGame>>

    // ─── Espansioni possedute ─────────────────────────────────────────────────
    /** Espansioni possedute collegate a un gioco base. */
    fun getOwnedExpansions(baseBggId: Long): Flow<List<BoardGame>>
    /** Id dei giochi base a cui un'espansione è collegata (vuoto = non posseduta). */
    fun getExpansionBaseLinks(expansionBggId: Long): Flow<List<Long>>
    /** Collega un'espansione posseduta a un gioco base. */
    suspend fun addOwnedExpansion(expansionBggId: Long, baseBggId: Long): Result<Unit>
    /** Rimuove un'espansione posseduta da tutti i suoi giochi base. */
    suspend fun removeOwnedExpansion(expansionBggId: Long): Result<Unit>
    /** True se il gioco è in libreria (usato per filtrare le basi possedute). */
    suspend fun isInLibrary(bggId: Long): Boolean
}