package com.kumadev.kumakeep.data.repository

import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.LibraryDao
import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.remote.api.BggApiService
import com.kumadev.kumakeep.data.remote.mapper.toEntity
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.LibraryEntry
import com.kumadev.kumakeep.domain.model.SearchResult
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BoardGameRepositoryImpl @Inject constructor(
    private val boardGameDao: BoardGameDao,
    private val libraryDao: LibraryDao,
    private val wishlistDao: com.kumadev.kumakeep.data.local.dao.WishlistDao,
    private val bggApiService: BggApiService
) : BoardGameRepository {

    override suspend fun searchBgg(query: String): Result<List<SearchResult>> {
        return runCatching {
            val response = bggApiService.search(query)
            response.items.map { item ->
                SearchResult(
                    bggId = item.id,
                    name = item.name?.value ?: "",
                    yearPublished = item.yearPublished?.value
                )
            }
        }
    }

    override suspend fun getGameDetail(bggId: Long): Result<BoardGame> {
        return runCatching {
            val cached = boardGameDao.getByBggId(bggId)
            val libraryEntry = libraryDao.getByBggId(bggId)

            // cache presente e ancora valida (TTL adattivo) → usa la copia locale
            if (cached != null && !cached.isCacheStale()) {
                return Result.success(cached.toDomain(libraryEntry))
            }

            // cache assente o scaduta → prova a rinfrescare da BGG
            try {
                val response = bggApiService.getGameDetail(bggId)
                val item = response.items.firstOrNull()
                    ?: error("Gioco non trovato su BGG")
                val fresh = item.toEntity()

                if (cached != null) {
                    // Update in-place: preserva id/createdAt e NON esegue il DELETE+INSERT
                    // di insertOrReplace (REPLACE), che con la FK onDelete=CASCADE
                    // cancellerebbe a cascata la library entry / le wishlist entry collegate.
                    val refreshed = fresh.copy(
                        id = cached.id,
                        createdAt = cached.createdAt,
                        updatedAt = System.currentTimeMillis()
                    )
                    boardGameDao.update(refreshed)
                    refreshed.toDomain(libraryEntry)
                } else {
                    boardGameDao.insertOrReplace(fresh)
                    fresh.toDomain(libraryEntry)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Rete assente / errore: se abbiamo una copia (anche stantia) la mostriamo,
                // altrimenti propaghiamo l'errore.
                cached?.toDomain(libraryEntry) ?: throw e
            }
        }
    }

    override fun getLibraryGames(): Flow<List<BoardGame>> {
        return libraryDao.getAll().map { libraryEntries ->
            libraryEntries.mapNotNull { entry ->
                val game = boardGameDao.getByBggId(entry.bggId) ?: return@mapNotNull null
                game.toDomain(entry)
            }
        }
    }

    override fun getPlayedCount(): Flow<Int> = libraryDao.getPlayedCount()

    override fun getWishlistGameCount(): Flow<Int> = wishlistDao.getWishlistGameCount()

    override fun getRecentlyViewedGames(bggIds: List<Long>): Flow<List<BoardGame>> {
        if (bggIds.isEmpty()) return kotlinx.coroutines.flow.flowOf(emptyList())
        return boardGameDao.getByBggIds(bggIds).map { entities ->
            val entityMap = entities.associateBy { it.bggId }
            bggIds.mapNotNull { id -> entityMap[id]?.toDomain(null) }
        }
    }
}

// ─── TTL adattivo per la cache dei dettagli ───────────────────────────────────
// La freschezza richiesta dipende dalla volatilità del dato: un gioco in uscita o
// appena pubblicato cambia spesso (rating in formazione, descrizione, player count),
// uno vecchio è di fatto congelato. Il costo API è trascurabile (refresh lazy, solo
// all'apertura del dettaglio), quindi il TTL ottimizza la freschezza, non le chiamate.
private const val DAY_MS = 24L * 60 * 60 * 1000

private fun BoardGameEntity.isCacheStale(now: Long = System.currentTimeMillis()): Boolean =
    now - updatedAt > cacheTtlMillis()

private fun BoardGameEntity.cacheTtlMillis(): Long {
    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    return when {
        // in uscita o pubblicato quest'anno (anno mancante = prudenziale)
        yearPublished == null || yearPublished >= currentYear -> 1 * DAY_MS
        // uscito negli ultimi ~2 anni
        yearPublished >= currentYear - 2 -> 7 * DAY_MS
        // gioco consolidato
        else -> 90 * DAY_MS
    }
}

// extension per convertire Entity → Domain
private fun BoardGameEntity.toDomain(
    libraryEntity: com.kumadev.kumakeep.data.local.entity.LibraryEntity?
): BoardGame {
    return BoardGame(
        bggId = bggId,
        primaryName = primaryName,
        yearPublished = yearPublished,
        minPlayers = minPlayers,
        maxPlayers = maxPlayers,
        minAge = minAge,
        playingTime = playingTime,
        complexity = complexity,
        bggRating = bggRating,
        thumbnail = thumbnail,
        image = image,
        description = description,
        designers = designers?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        publishers = publishers?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        categories = categories?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        mechanics = mechanics?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        libraryEntry = libraryEntity?.let {
            LibraryEntry(
                id = it.id,
                rate = it.rate,
                numPlays = it.numPlays,
                notes = it.notes,
                createdAt = it.createdAt
            )
        }
    )
}