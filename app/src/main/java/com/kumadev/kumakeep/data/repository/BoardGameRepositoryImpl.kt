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
            // prima controlla la cache locale
            val cached = boardGameDao.getByBggId(bggId)
            if (cached != null) {
                val libraryEntry = libraryDao.getByBggId(bggId)
                return Result.success(cached.toDomain(libraryEntry))
            }
            // altrimenti chiama BGG e salva in cache
            val response = bggApiService.getGameDetail(bggId)
            val item = response.items.firstOrNull()
                ?: error("Gioco non trovato su BGG")
            val entity = item.toEntity()
            boardGameDao.insertOrReplace(entity)
            val libraryEntry = libraryDao.getByBggId(bggId)
            entity.toDomain(libraryEntry)
        }
    }

    override fun getLibraryGames(): Flow<List<BoardGame>> {
        return combine(
            libraryDao.getAll(),
            boardGameDao.getByBggIds(emptyList()) // placeholder, vedi sotto
        ) { _, _ -> emptyList() } // implementazione completa nel prossimo step
    }

    override fun searchLibrary(query: String): Flow<List<BoardGame>> {
        return boardGameDao.searchLocal(query).map { entities ->
            entities.map { it.toDomain(null) }
        }
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
                rate = it.rate.name,
                numPlays = it.numPlays.name,
                notes = it.notes
            )
        }
    )
}