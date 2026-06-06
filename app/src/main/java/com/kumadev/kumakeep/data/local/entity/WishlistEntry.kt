package com.kumadev.kumakeep.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wishlists",
    indices = [Index(value = ["name"], unique = true)]
)
data class WishlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "wishlist_entries",
    indices = [Index(value = ["wishlistId", "bggId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = WishlistEntity::class,
            parentColumns = ["id"],
            childColumns = ["wishlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BoardGameEntity::class,
            parentColumns = ["bggId"],
            childColumns = ["bggId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WishlistEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wishlistId: Long,
    val bggId: Long,
    val toBuy: Boolean = false,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)