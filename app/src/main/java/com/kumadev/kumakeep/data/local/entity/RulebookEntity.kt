package com.kumadev.kumakeep.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rulebooks",
    indices = [Index(value = ["gameId"], unique = true)]
)
data class RulebookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val filePath: String,
    val fileName: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val importedAt: Long = System.currentTimeMillis()
)
