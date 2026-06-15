package com.kumadev.kumakeep.domain.usecase

import com.kumadev.kumakeep.data.local.dao.WishlistDao
import com.kumadev.kumakeep.domain.model.WishlistEntry
import javax.inject.Inject

class ReorderWishlistUseCase @Inject constructor(
    private val wishlistDao: WishlistDao
) {
    /**
     * Persiste il nuovo ordine degli elementi. [entries] è la lista nell'ordine finale desiderato.
     * Aggiorna sortOrder di ogni entry in base alla sua posizione nell'array.
     */
    suspend operator fun invoke(wishlistId: Long, entries: List<WishlistEntry>): Result<Unit> =
        runCatching {
            val current = wishlistDao.getEntriesByWishlistSnapshot(wishlistId)
            val byEntryId = current.associateBy { it.id }
            val updated = entries.mapIndexed { index, entry ->
                byEntryId[entry.entryId]?.copy(sortOrder = index)
                    ?: error("Entry ${entry.entryId} non trovata")
            }
            wishlistDao.updateEntries(updated)
        }
}
