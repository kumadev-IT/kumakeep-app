package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.data.local.entity.WishlistEntryEntity
import com.kumadev.kumakeep.data.remote.api.BggApiService
import com.kumadev.kumakeep.data.remote.mapper.toEntity
import javax.inject.Inject

/**
 * Aggiunge un singolo gioco a una wishlist specifica.
 * Assicura la cache locale del gioco prima di inserire l'entry.
 * Usato dal flusso "cerca e aggiungi" dentro WishlistDetail.
 */
class AddGameToWishlistUseCase @Inject constructor(
    private val wishlistDao: WishlistDao,
    private val boardGameDao: BoardGameDao,
    private val bggApiService: BggApiService
) {
    suspend operator fun invoke(bggId: Long, wishlistId: Long): Result<Unit> =
        runCatching {
            val alreadyIn = wishlistDao.existsEntry(bggId, wishlistId)
            if (alreadyIn) return Result.success(Unit)

            // Assicura cache locale
            if (boardGameDao.getByBggId(bggId) == null) {
                val response = bggApiService.getGameDetail(bggId)
                val item = response.items.firstOrNull() ?: error("Gioco non trovato su BGG")
                boardGameDao.insertOrReplace(item.toEntity())
            }

            val maxOrder = wishlistDao.getMaxSortOrder(wishlistId) ?: -1
            wishlistDao.insertEntry(
                WishlistEntryEntity(
                    wishlistId = wishlistId,
                    bggId = bggId,
                    sortOrder = maxOrder + 1
                )
            )
        }
}
