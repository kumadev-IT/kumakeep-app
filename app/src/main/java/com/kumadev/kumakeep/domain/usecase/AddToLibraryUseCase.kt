package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.LibraryDao
import com.kumadev.kumakeep.data.local.entity.LibraryEntity
import com.kumadev.kumakeep.data.remote.api.BggApiService
import com.kumadev.kumakeep.data.remote.mapper.toEntity
import javax.inject.Inject

class AddToLibraryUseCase @Inject constructor(
    private val libraryDao: LibraryDao,
    private val boardGameDao: BoardGameDao,
    private val bggApiService: BggApiService
) {
    suspend operator fun invoke(bggId: Long): Result<Unit> {
        return runCatching {
            // assicura che il gioco sia in cache locale
            if (boardGameDao.getByBggId(bggId) == null) {
                val response = bggApiService.getGameDetail(bggId)
                val item = response.items.firstOrNull()
                    ?: error("Gioco non trovato su BGG")
                boardGameDao.insertOrReplace(item.toEntity())
            }
            libraryDao.insert(LibraryEntity(bggId = bggId))
        }
    }
}