package com.kumadev.rulesreader.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "extracted_pages",
    indices = [Index(value = ["rulebookId", "pageNum"], unique = true)]
)
data class ExtractedPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rulebookId: Long,
    val pageNum: Int,
    val rawText: String
)
