package com.kumadev.kumakeep.domain.repository

import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.SearchResult
import kotlinx.coroutines.flow.Flow

interface BoardGameRepository {
    suspend fun searchBgg(query: String): Result<List<SearchResult>>
    suspend fun getGameDetail(bggId: Long): Result<BoardGame>
    fun getLibraryGames(): Flow<List<BoardGame>>
    fun searchLibrary(query: String): Flow<List<BoardGame>>
    fun getPlayedCount(): Flow<Int>
    fun getWishlistGameCount(): Flow<Int>
    fun getRecentlyViewedGames(bggIds: List<Long>): Flow<List<BoardGame>>
}