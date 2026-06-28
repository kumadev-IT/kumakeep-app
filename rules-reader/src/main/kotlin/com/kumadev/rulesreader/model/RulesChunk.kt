package com.kumadev.rulesreader.model

data class RulesChunk(
    val index: Int,
    /** Numero di pagina del primo token del chunk. */
    val pageNum: Int,
    val text: String,
    /** Tipo di sezione logica di appartenenza (vedi [com.kumadev.rulesreader.chunker.SectionType]). Null se non classificato. */
    val sectionType: String? = null
)
