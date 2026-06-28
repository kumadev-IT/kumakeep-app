package com.kumadev.rulesreader.model

data class RulesChunk(
    val index: Int,
    /** Numero di pagina del primo token del chunk. */
    val pageNum: Int,
    val text: String
)
