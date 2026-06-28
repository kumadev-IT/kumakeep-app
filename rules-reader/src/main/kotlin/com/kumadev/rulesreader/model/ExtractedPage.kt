package com.kumadev.rulesreader.model

import com.kumadev.rulesreader.db.entity.ExtractedPageEntity

data class ExtractedPage(
    val pageNum: Int,
    val rawText: String
) {
    /** True se la pagina non contiene testo selezionabile (PDF-immagine). */
    val isEmpty: Boolean get() = rawText.isBlank()

    fun toEntity(rulebookId: Long) = ExtractedPageEntity(
        rulebookId = rulebookId,
        pageNum = pageNum,
        rawText = rawText
    )
}
