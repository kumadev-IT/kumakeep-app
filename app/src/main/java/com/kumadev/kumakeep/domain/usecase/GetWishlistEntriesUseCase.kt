package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.domain.model.WishlistEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetWishlistEntriesUseCase @Inject constructor(
    private val wishlistDao: WishlistDao
) {
    operator fun invoke(wishlistId: Long): Flow<List<WishlistEntry>> =
        wishlistDao.getEntriesWithGame(wishlistId).map { rows ->
            rows.map {
                WishlistEntry(
                    entryId = it.entryId,
                    wishlistId = it.wishlistId,
                    bggId = it.bggId,
                    toBuy = it.toBuy,
                    notes = it.notes,
                    sortOrder = it.sortOrder,
                    primaryName = it.primaryName,
                    thumbnail = it.thumbnail,
                    yearPublished = it.yearPublished
                )
            }
        }
}
