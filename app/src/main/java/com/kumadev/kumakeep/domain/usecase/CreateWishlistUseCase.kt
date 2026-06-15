package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.data.local.entity.WishlistEntity
import javax.inject.Inject

class CreateWishlistUseCase @Inject constructor(
    private val wishlistDao: WishlistDao
) {
    suspend operator fun invoke(name: String, description: String? = null): Result<Long> =
        runCatching {
            wishlistDao.insertWishlist(WishlistEntity(name = name.trim(), description = description?.trim()))
        }
}
