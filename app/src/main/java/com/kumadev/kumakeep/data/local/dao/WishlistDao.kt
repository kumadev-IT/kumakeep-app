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

    @Query("SELECT * FROM wishlists")
    fun getAllWishlists(): Flow<List<WishlistEntity>>

    @Query("SELECT * FROM wishlists WHERE id = :id")
    suspend fun getWishlistById(id: Long): WishlistEntity?

    // --- WishlistEntry (i giochi dentro le liste) ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEntry(entry: WishlistEntryEntity)

    @Update
    suspend fun updateEntry(entry: WishlistEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: WishlistEntryEntity)

    @Query("SELECT * FROM wishlist_entries WHERE wishlistId = :wishlistId")
    fun getEntriesByWishlist(wishlistId: Long): Flow<List<WishlistEntryEntity>>

    @Query("SELECT * FROM wishlist_entries WHERE toBuy = 1")
    fun getToBuyEntries(): Flow<List<WishlistEntryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_entries WHERE bggId = :bggId)")
    fun isInAnyWishlist(bggId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM wishlist_entries WHERE bggId = :bggId AND wishlistId = :wishlistId)")
    fun isInWishlist(bggId: Long, wishlistId: Long): Flow<Boolean>

    @Query("DELETE FROM wishlist_entries WHERE bggId = :bggId AND wishlistId = :wishlistId")
    suspend fun deleteEntryByBggIdAndWishlist(bggId: Long, wishlistId: Long)
}