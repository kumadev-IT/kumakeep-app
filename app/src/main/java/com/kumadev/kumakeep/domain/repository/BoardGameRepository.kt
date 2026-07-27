package com.kumadev.kumakeep.domain.repository

import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.SearchResult
import com.kumadev.kumakeep.domain.model.Tag
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
    /** Numero totale di espansioni distinte possedute (per la Home). */
    fun getOwnedExpansionCount(): Flow<Int>
    /** Collega un'espansione posseduta a un gioco base. */
    suspend fun addOwnedExpansion(expansionBggId: Long, baseBggId: Long): Result<Unit>
    /** Rimuove un'espansione posseduta da tutti i suoi giochi base. */
    suspend fun removeOwnedExpansion(expansionBggId: Long): Result<Unit>
    /** True se il gioco è in libreria (usato per filtrare le basi possedute). */
    suspend fun isInLibrary(bggId: Long): Boolean

    // ─── Tag utente ─────────────────────────────────────────────────────────
    /** Tutti i tag definiti dall'utente (per il picker). */
    fun getAllTags(): Flow<List<Tag>>
    /** Tag assegnati a un gioco specifico. */
    fun getTagsForGame(bggId: Long): Flow<List<Tag>>
    /** Crea un nuovo tag. Fallisce se il nome esiste già. */
    suspend fun createTag(name: String, colorHex: String): Result<Tag>
    /** Rinomina/ricolora un tag esistente. */
    suspend fun updateTag(tagId: Long, name: String, colorHex: String): Result<Unit>
    /** Elimina un tag (rimuove anche tutte le associazioni ai giochi). */
    suspend fun deleteTag(tagId: Long): Result<Unit>
    /** Assegna un tag esistente a un gioco. */
    suspend fun assignTagToGame(bggId: Long, tagId: Long): Result<Unit>
    /** Rimuove un tag da un gioco. */
    suspend fun removeTagFromGame(bggId: Long, tagId: Long): Result<Unit>
}