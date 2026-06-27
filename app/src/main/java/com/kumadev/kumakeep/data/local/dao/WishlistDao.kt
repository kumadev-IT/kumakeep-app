package com.kumadev.kumakeep.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.kumadev.kumakeep.data.local.entity.WishlistEntity
import com.kumadev.kumakeep.data.local.entity.WishlistEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WishlistDao {

    // --- Wishlist (le liste) ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertWishlist(wishlist: WishlistEntity): Long

    @Update
    suspend fun updateWishlist(wishlist: WishlistEntity)

    @Delete
    suspend fun deleteWishlist(wishlist: WishlistEntity)

    @Query("SELECT * FROM wishlists WHERE id = :id")
    suspend fun getWishlistById(id: Long): WishlistEntity?

    @Query("""
        SELECT w.id, w.name, w.description, w.createdAt, COUNT(we.id) AS entryCount
        FROM wishlists w
        LEFT JOIN wishlist_entries we ON w.id = we.wishlistId
        GROUP BY w.id
        ORDER BY w.name ASC
    """)
    fun getAllWishlistsWithCount(): Flow<List<WishlistWithCount>>

    // Per il dialog multi-select in GameDetail
    @Query("""
        SELECT w.id, w.name,
               EXISTS(SELECT 1 FROM wishlist_entries we WHERE we.wishlistId = w.id AND we.bggId = :bggId) AS isSelected
        FROM wishlists w
        ORDER BY w.name ASC
    """)
    fun getWishlistsWithStatusForGame(bggId: Long): Flow<List<WishlistSelectionRow>>

    // --- WishlistEntry (i giochi nelle liste) ---

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: WishlistEntryEntity)

    @Update
    suspend fun updateEntry(entry: WishlistEntryEntity)

    @Update
    suspend fun updateEntries(entries: List<WishlistEntryEntity>)

    @Delete
    suspend fun deleteEntry(entry: WishlistEntryEntity)

    @Query("DELETE FROM wishlist_entries WHERE bggId = :bggId AND wishlistId = :wishlistId")
    suspend fun deleteEntryByBggIdAndWishlist(bggId: Long, wishlistId: Long)

    // JOIN con boardgames per ottenere nome/thumbnail insieme all'entry
    @Query("""
        SELECT we.id AS entryId, we.wishlistId, we.bggId, we.toBuy, we.notes, we.sortOrder, we.createdAt,
               bg.primaryName, bg.thumbnail, bg.yearPublished
        FROM wishlist_entries we
        INNER JOIN boardgames bg ON we.bggId = bg.bggId
        WHERE we.wishlistId = :wishlistId
        ORDER BY we.sortOrder ASC
    """)
    fun getEntriesWithGame(wishlistId: Long): Flow<List<WishlistEntryWithGame>>

    // Usato per calcolare sortOrder del prossimo inserimento (append in fondo)
    @Query("SELECT MAX(sortOrder) FROM wishlist_entries WHERE wishlistId = :wishlistId")
    suspend fun getMaxSortOrder(wishlistId: Long): Int?

    // Per recuperare una singola entry (necessario per toggle toBuy)
    @Query("SELECT * FROM wishlist_entries WHERE id = :entryId")
    suspend fun getEntryById(entryId: Long): WishlistEntryEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_entries WHERE bggId = :bggId AND wishlistId = :wishlistId)")
    suspend fun existsEntry(bggId: Long, wishlistId: Long): Boolean

    @Query("SELECT * FROM wishlist_entries WHERE wishlistId = :wishlistId ORDER BY sortOrder ASC")
    suspend fun getEntriesByWishlistSnapshot(wishlistId: Long): List<WishlistEntryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_entries WHERE bggId = :bggId)")
    fun isInAnyWishlist(bggId: Long): Flow<Boolean>

    @Query("SELECT COUNT(DISTINCT bggId) FROM wishlist_entries")
    fun getWishlistGameCount(): Flow<Int>
}
