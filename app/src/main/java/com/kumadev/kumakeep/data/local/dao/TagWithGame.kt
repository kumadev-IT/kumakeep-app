package com.kumadev.kumakeep.data.local.dao

/** Riga per il fetch bulk dei tag su più giochi (es. lista libreria). */
data class GameTagRow(
    val bggId: Long,
    val tagId: Long,
    val name: String,
    val colorHex: String
)
