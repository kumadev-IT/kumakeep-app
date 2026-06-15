package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.BoardGameDao
import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.data.local.entity.WishlistEntryEntity
import com.kumadev.kumakeep.data.remote.api.BggApiService
import com.kumadev.kumakeep.data.remote.mapper.toEntity
import javax.inject.Inject

class AddToWishlistsUseCase @Inject constructor(
    private val wishlistDao: WishlistDao,
    private val boardGameDao: BoardGameDao,
    private val bggApiService: BggApiService
) {
    /**
     * Aggiunge bggId alle wishlist indicate. Rimuove dalle wishlist deselezionate.
     * [selectedIds] = set finale delle wishlist in cui il gioco deve essere presente.
     * [previousIds] = set delle wishlist in cui era presente prima (per calcolare le rimozioni).
     */
    suspend operator fun invoke(
        bggId: Long,
        selectedIds: Set<Long>,
        previousIds: Set<Long>
    ): Result<Unit> = runCatching {
        // Assicura cache locale
        if (boardGameDao.getByBggId(bggId) == null) {
            val response = bggApiService.getGameDetail(bggId)
            val item = response.items.firstOrNull() ?: error("Gioco non trovato su BGG")
            boardGameDao.insertOrReplace(item.toEntity())
        }

        val toAdd = selectedIds - previousIds
        val toRemove = previousIds - selectedIds

        for (wishlistId in toAdd) {
            val maxOrder = wishlistDao.getMaxSortOrder(wishlistId) ?: -1
            wishlistDao.insertEntry(
                WishlistEntryEntity(
                    wishlistId = wishlistId,
                    bggId = bggId,
                    sortOrder = maxOrder + 1
                )
            )
        }

        for (wishlistId in toRemove) {
            wishlistDao.deleteEntryByBggIdAndWishlist(bggId, wishlistId)
        }
    }
}
