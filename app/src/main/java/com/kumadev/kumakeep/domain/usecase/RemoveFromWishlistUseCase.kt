package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.WishlistDao
import javax.inject.Inject

class RemoveFromWishlistUseCase @Inject constructor(
    private val wishlistDao: WishlistDao
) {
    suspend operator fun invoke(bggId: Long, wishlistId: Long): Result<Unit> =
        runCatching {
            wishlistDao.deleteEntryByBggIdAndWishlist(bggId, wishlistId)
        }
}
