package com.kumadev.kumakeep.domain.model

data class Wishlist(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val entryCount: Int
)

data class WishlistEntry(
    val entryId: Long,
    val wishlistId: Long,
    val bggId: Long,
    val toBuy: Boolean,
    val notes: String?,
    val sortOrder: Int,
    val primaryName: String,
    val thumbnail: String?,
    val yearPublished: Int?
)

data class WishlistWithStatus(
    val id: Long,
    val name: String,
    val isSelected: Boolean
)
