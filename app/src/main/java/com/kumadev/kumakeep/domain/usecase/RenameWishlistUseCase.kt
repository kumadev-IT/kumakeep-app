package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.WishlistDao
import javax.inject.Inject

class RenameWishlistUseCase @Inject constructor(
    private val wishlistDao: WishlistDao
) {
    suspend operator fun invoke(id: Long, newName: String): Result<Unit> =
        runCatching {
            val existing = wishlistDao.getWishlistById(id) ?: error("Wishlist non trovata")
            wishlistDao.updateWishlist(existing.copy(name = newName.trim()))
        }
}
