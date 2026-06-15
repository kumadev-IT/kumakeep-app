package com.kumadev.kumakeep.data.local.dao

data class WishlistEntryWithGame(
    val entryId: Long,
    val wishlistId: Long,
    val bggId: Long,
    val toBuy: Boolean,
    val notes: String?,
    val sortOrder: Int,
    val createdAt: Long,
    val primaryName: String,
    val thumbnail: String?,
    val yearPublished: Int?
)

data class WishlistWithCount(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val entryCount: Int
)

data class WishlistSelectionRow(
    val id: Long,
    val name: String,
    val isSelected: Boolean
)
