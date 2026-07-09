package com.kumadev.rulesreader.chunker

import com.kumadev.rulesreader.model.ExtractedPage
import com.kumadev.rulesreader.model.RulesChunk
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Segmenta il testo estratto in chunk con sliding window + overlap,
 * preservando il numero di pagina di origine per ogni chunk.
 *
 * Algoritmo word-based (non tokenizer):
 * - Chunk target: [CHUNK_WORDS] parole (~500)
 * - Overlap: [OVERLAP_WORDS] parole (~100) → finestra scorrevole
 *
 * Le pagine vuote vengono saltate.
 */
@Singleton
class RulesChunker @Inject constructor() {

    companion object {
        const val CHUNK_WORDS = 500
        const val OVERLAP_WORDS = 100
    }

    /**
     * @param pages Lista ordinata per pageNum di pagine estratte.
     * @return Lista di [RulesChunk] con indice progressivo e numero di pagina.
     */
    fun chunk(pages: List<ExtractedPage>): List<RulesChunk> {
        // Costruisce una sequenza flat di (word, pageNum)
        data class WordEntry(val word: String, val pageNum: Int)

        val words: List<WordEntry> = pages
            .filter { it.rawText.isNotBlank() }
            .flatMap { page ->
                page.rawText.split(Regex("\\s+"))
                    .filter { it.isNotEmpty() }
                    .map { WordEntry(it, page.pageNum) }
            }

        if (words.isEmpty()) return emptyList()

        val chunks = mutableListOf<RulesChunk>()
        var start = 0
        var chunkIndex = 0

        while (start < words.size) {
            val end = minOf(start + CHUNK_WORDS, words.size)
            val slice = words.subList(start, end)

            val text = slice.joinToString(" ") { it.word }
            val pageNum = slice.first().pageNum

            chunks.add(RulesChunk(index = chunkIndex, pageNum = pageNum, text = text))
            chunkIndex++

            // Avanza di (CHUNK_WORDS - OVERLAP_WORDS) → sliding window
            val step = CHUNK_WORDS - OVERLAP_WORDS
            start += step

            // Evita chunk finali troppo piccoli (< 20 parole): li fondiamo all'ultimo
            if (start < words.size && (words.size - start) < 20) break
        }

        return chunks
    }
}
