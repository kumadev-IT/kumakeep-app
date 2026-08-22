package com.kumadev.kumakeep.data.repository

import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.GameTagRow
import com.kumadev.kumakeep.data.local.dao.LibraryDao
import com.kumadev.kumakeep.data.local.dao.TagDao
import com.kumadev.kumakeep.data.local.entity.BoardGameEntity
import com.kumadev.kumakeep.data.local.entity.GameTagEntity
import com.kumadev.kumakeep.data.local.entity.TagEntity
import com.kumadev.kumakeep.data.remote.api.BggApiService
import com.kumadev.kumakeep.data.remote.mapper.toDomain
import com.kumadev.kumakeep.data.remote.mapper.toEntity
import com.kumadev.kumakeep.domain.model.BoardGame
import com.kumadev.kumakeep.domain.model.HotGame
import com.kumadev.kumakeep.domain.model.LibraryEntry
import com.kumadev.kumakeep.domain.model.SearchResult
import com.kumadev.kumakeep.domain.model.Tag
import com.kumadev.kumakeep.domain.repository.BoardGameRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BoardGameRepositoryImpl @Inject constructor(
    private val boardGameDao: BoardGameDao,
    private val libraryDao: LibraryDao,
    private val wishlistDao: com.kumadev.kumakeep.data.local.dao.WishlistDao,
    private val ownedExpansionDao: com.kumadev.kumakeep.data.local.dao.OwnedExpansionDao,
    private val tagDao: TagDao,
    private val bggApiService: BggApiService
) : BoardGameRepository {

    // Cache in-memoria per la hot list: chiamata di rete "leggera" ma comunque
    // non reattiva come le altre liste della Home — un TTL breve evita di
    // richiamarla ad ogni ritorno sulla tab Home nella stessa sessione app
    // (il ViewModel Home sopravvive ai cambi tab, vedi punto 26 del todo).
    private var hotGamesCache: List<HotGame>? = null
    private var hotGamesCachedAt: Long = 0L

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
                    val isExpansion = item.id in expansionIds
                    val isOwned = if (isExpansion) {
                        ownedExpansionDao.isOwned(item.id)
                    } else {
                        libraryDao.getByBggId(item.id) != null
                    }
                    seen[item.id] = SearchResult(
                        bggId = item.id,
                        name = item.name?.value ?: "",
                        yearPublished = item.yearPublished?.value,
                        isExpansion = isExpansion,
                        isOwned = isOwned
                    )
                }
            }
            // Ordinamento euristico (punto 25): match esatto sempre primo, poi
            // prefix match (nome che inizia con la query), poi anno decrescente
            // come ultimo criterio. Nessuna chiamata di rete aggiuntiva: usa solo
            // dati già presenti nella risposta di /search.
            val normalizedQuery = query.trim().lowercase()
            seen.values.sortedWith(
                compareBy<SearchResult> { result ->
                    val normalizedName = result.name.trim().lowercase()
                    when {
                        normalizedName == normalizedQuery -> 0
                        normalizedName.startsWith(normalizedQuery) -> 1
                        else -> 2
                    }
                }.thenByDescending { it.yearPublished ?: Int.MIN_VALUE }
            )
        }
    }

    override suspend fun getGameDetail(bggId: Long): Result<BoardGame> {
        return runCatching {
            val cached = boardGameDao.getByBggId(bggId)
            val libraryEntry = libraryDao.getByBggId(bggId)
            val tags = tagDao.getTagsForGame(bggId).first().map { it.toDomain() }

            // cache presente e ancora valida (TTL adattivo) → usa la copia locale
            if (cached != null && !cached.isCacheStale()) {
                return Result.success(cached.toDomain(libraryEntry, tags))
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
                    refreshed.toDomain(libraryEntry, tags)
                } else {
                    boardGameDao.insertOrReplace(fresh)
                    fresh.toDomain(libraryEntry, tags)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Rete assente / errore: se abbiamo una copia (anche stantia) la mostriamo,
                // altrimenti propaghiamo l'errore.
                cached?.toDomain(libraryEntry, tags) ?: throw e
            }
        }
    }

    override fun getLibraryGames(): Flow<List<BoardGame>> {
        // Batch invece di N+1: prima erano una query boardGameDao.getByBggId() PER OGNI
        // riga di libreria (410+ query sequenziali con la collezione importata da CSV),
        // causa principale della lentezza percepita all'apertura di Home/Library.
        // Ora un'unica query IN (...) per i giochi + una per i tag, combinate.
        return libraryDao.getAll().flatMapLatest { libraryEntries ->
            val bggIds = libraryEntries.map { it.bggId }
            if (bggIds.isEmpty()) return@flatMapLatest flowOf(emptyList())
            combine(
                boardGameDao.getByBggIds(bggIds),
                tagDao.getTagsForGames(bggIds)
            ) { games, tagRows ->
                val gamesById = games.associateBy { it.bggId }
                val tagsByGame = tagRows.groupBy(GameTagRow::bggId)
                libraryEntries.mapNotNull { entry ->
                    val game = gamesById[entry.bggId] ?: return@mapNotNull null
                    val tags = tagsByGame[entry.bggId]?.map { it.toDomain() } ?: emptyList()
                    game.toDomain(entry, tags)
                }
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

    override suspend fun getHotGames(): Result<List<HotGame>> {
        val cached = hotGamesCache
        if (cached != null && System.currentTimeMillis() - hotGamesCachedAt < HOT_GAMES_CACHE_TTL_MS) {
            return Result.success(cached)
        }
        return try {
            val fresh = bggApiService.getHotGames().items.map { it.toDomain() }
            hotGamesCache = fresh
            hotGamesCachedAt = System.currentTimeMillis()
            Result.success(fresh)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            // Rete assente: una copia scaduta è comunque meglio di niente, se disponibile.
            cached?.let { Result.success(it) } ?: Result.failure(e)
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

    // ─── Tag utente ─────────────────────────────────────────────────────────

    override fun getAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { list -> list.map { it.toDomain() } }

    override fun getTagsForGame(bggId: Long): Flow<List<Tag>> =
        tagDao.getTagsForGame(bggId).map { list -> list.map { it.toDomain() } }

    override suspend fun createTag(name: String, colorHex: String): Result<Tag> =
        runCatching {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Il nome del tag non può essere vuoto" }
            if (tagDao.existsByName(trimmed)) error("Esiste già un tag chiamato \"$trimmed\"")
            val id = tagDao.insertTag(TagEntity(name = trimmed, colorHex = colorHex))
            Tag(id = id, name = trimmed, colorHex = colorHex)
        }

    override suspend fun updateTag(tagId: Long, name: String, colorHex: String): Result<Unit> =
        runCatching {
            val trimmed = name.trim()
            require(trimmed.isNotEmpty()) { "Il nome del tag non può essere vuoto" }
            tagDao.updateTag(TagEntity(id = tagId, name = trimmed, colorHex = colorHex))
        }

    override suspend fun deleteTag(tagId: Long): Result<Unit> =
        runCatching { tagDao.deleteTag(tagId) }

    override suspend fun assignTagToGame(bggId: Long, tagId: Long): Result<Unit> =
        runCatching { tagDao.assignTag(GameTagEntity(bggId = bggId, tagId = tagId)) }

    override suspend fun removeTagFromGame(bggId: Long, tagId: Long): Result<Unit> =
        runCatching { tagDao.removeTag(bggId, tagId) }

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

// TTL per la cache in-memoria della hot list BGG: dato che cambia un paio di
// volte al giorno lato BGG, 1h basta ad evitare chiamate ripetute nella stessa
// sessione senza mostrare mai un ranking vistosamente stantio.
private const val HOT_GAMES_CACHE_TTL_MS = 60L * 60 * 1000

private fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, colorHex = colorHex)

private fun GameTagRow.toDomain(): Tag = Tag(id = tagId, name = name, colorHex = colorHex)

// extension per convertire Entity → Domain
private fun BoardGameEntity.toDomain(
    libraryEntity: com.kumadev.kumakeep.data.local.entity.LibraryEntity?,
    tags: List<Tag> = emptyList()
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
        },
        tags = tags
    )
}
