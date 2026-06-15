package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.domain.model.Wishlist
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetWishlistsUseCase @Inject constructor(
    private val wishlistDao: WishlistDao
) {
    operator fun invoke(): Flow<List<Wishlist>> =
        wishlistDao.getAllWishlistsWithCount().map { rows ->
            rows.map { Wishlist(it.id, it.name, it.description, it.createdAt, it.entryCount) }
        }
}
