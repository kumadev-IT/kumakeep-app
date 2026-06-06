package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.LibraryDao
import javax.inject.Inject

class RemoveFromLibraryUseCase @Inject constructor(
    private val libraryDao: LibraryDao
) {
    suspend operator fun invoke(bggId: Long): Result<Unit> {
        return runCatching {
            val entry = libraryDao.getByBggId(bggId)
                ?: error("Gioco non in libreria")
            libraryDao.delete(entry)
        }
    }
}