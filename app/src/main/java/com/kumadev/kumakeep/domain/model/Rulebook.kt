package com.kumadev.kumakeep.domain.model

data class Rulebook(
    val id: Long,
    val gameId: Long,
    val filePath: String,
    val fileName: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val importedAt: Long
)
