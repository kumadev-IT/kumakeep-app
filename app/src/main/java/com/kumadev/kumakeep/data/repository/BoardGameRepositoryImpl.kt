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
    private val ownedExpansionDao: com.kumadev.kumakeep.data.local.dao.OwnedExpansionDao,
    private val bggApiService: BggApiService
) : BoardGameRepository {

    override suspend fun searchBgg(query: String): Result<List<SearchResult>> {
        return runCatching {
            val items = bggApiService.search(query).items
            // Un id è espansione se compare almeno una volta nel bucket
            // "boardgameexpansion" (membership affidabile, a differenza del type
            // della singola riga). Gli accessori non sono gestiti per ora.
            val expansionIds = items
                .filter { it.type == "boardgameexpansion" }
                .map { it.id }
                .toSet()
            // Dedup per id preservando l'ordine di prima comparsa.
            val seen = LinkedHashMap<Long, SearchResult>()
            for (item in items) {
                if (!seen.containsKey(item.id)) {
                    seen[item.id] = SearchResult(
                        bggId = item.id,
                        name = item.name?.value ?: "",
                        yearPublished = item.yearPublished?.value,
                        isExpansion = item.id in expansionIds
                    )
                }
            }
            seen.values.toList()
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

    // ─── Espansioni possedute ─────────────────────────────────────────────────

    override fun getOwnedExpansions(baseBggId: Long): Flow<List<BoardGame>> =
        ownedExpansionDao.getExpansionsForBase(baseBggId).map { list ->
            list.map { it.toDomain(null) }
        }

    override fun getExpansionBaseLinks(expansionBggId: Long): Flow<List<Long>> =
        ownedExpansionDao.getBaseIdsForExpansion(expansionBggId)

    override fun getOwnedExpansionCount(): Flow<Int> =
        ownedExpansionDao.getOwnedExpansionCount()

    override suspend fun addOwnedExpansion(expansionBggId: Long, baseBggId: Long): Result<Unit> {
        return runCatching {
            // entrambi i giochi devono esistere in `boardgames` (FK). Di norma lo
            // sono già (espansione = dettaglio aperto, base = in libreria), ma per
            // sicurezza li rifornisco da BGG se mancanti.
            ensureCached(expansionBggId)
            ensureCached(baseBggId)
            ownedExpansionDao.insert(
                com.kumadev.kumakeep.data.local.entity.OwnedExpansionEntity(
                    expansionBggId = expansionBggId,
                    baseBggId = baseBggId
                )
            )
        }
    }

    override suspend fun removeOwnedExpansion(expansionBggId: Long): Result<Unit> =
        runCatching { ownedExpansionDao.unlinkAll(expansionBggId) }

    override suspend fun isInLibrary(bggId: Long): Boolean =
        libraryDao.getByBggId(bggId) != null

    /** Garantisce che il gioco sia in cache locale (scarica da BGG se assente). */
    private suspend fun ensureCached(bggId: Long) {
        if (boardGameDao.getByBggId(bggId) == null) {
            val response = bggApiService.getGameDetail(bggId)
            val item = response.items.firstOrNull() ?: error("Gioco non trovato su BGG")
            boardGameDao.insertOrReplace(item.toEntity())
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
        isExpansion = isExpansion,
        baseGames = com.kumadev.kumakeep.data.remote.mapper.parseBaseGames(baseGamesRef),
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