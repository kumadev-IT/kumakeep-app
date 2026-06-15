package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.WishlistDao
import javax.inject.Inject

class DeleteWishlistUseCase @Inject constructor(
    private val wishlistDao: WishlistDao
) {
    suspend operator fun invoke(id: Long): Result<Unit> =
        runCatching {
            val wishlist = wishlistDao.getWishlistById(id) ?: error("Wishlist non trovata")
            wishlistDao.deleteWishlist(wishlist)
        }
}
