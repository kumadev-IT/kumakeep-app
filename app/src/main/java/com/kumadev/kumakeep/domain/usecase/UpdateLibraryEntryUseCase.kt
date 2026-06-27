package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.LibraryDao
import com.kumadev.kumakeep.data.local.entity.NumPlays
import com.kumadev.kumakeep.data.local.entity.UserRate
import javax.inject.Inject

class UpdateLibraryEntryUseCase @Inject constructor(
    private val libraryDao: LibraryDao
) {
    suspend operator fun invoke(
        bggId: Long,
        rate: UserRate? = null,
        numPlays: NumPlays? = null
    ): Result<Unit> = runCatching {
        val current = libraryDao.getByBggId(bggId)
            ?: error("Gioco non in libreria")
        libraryDao.update(
            current.copy(
                rate = rate ?: current.rate,
                numPlays = numPlays ?: current.numPlays,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
