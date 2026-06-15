package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.domain.model.WishlistWithStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetWishlistsForGameUseCase @Inject constructor(
    private val wishlistDao: WishlistDao
) {
    operator fun invoke(bggId: Long): Flow<List<WishlistWithStatus>> =
        wishlistDao.getWishlistsWithStatusForGame(bggId).map { rows ->
            rows.map { WishlistWithStatus(it.id, it.name, it.isSelected) }
        }
}
