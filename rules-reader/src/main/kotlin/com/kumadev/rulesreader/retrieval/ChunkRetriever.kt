package com.kumadev.rulesreader.retrieval

import com.kumadev.rulesreader.db.RulesReaderDatabase
import com.kumadev.rulesreader.db.entity.RulebookChunkEntity
import com.kumadev.rulesreader.embedding.toFloatArray
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

/**
 * Recupera i chunk più rilevanti per una query tramite cosine similarity in-memory.
 *
 * Con 28 chunk e embedding dim=100, il carico in RAM è trascurabile (~11 KB).
 * Non serve un vector store esterno.
 */
@Singleton
class ChunkRetriever @Inject constructor(
    private val database: RulesReaderDatabase
) {

    /**
     * Restituisce i [topK] chunk più simili a [queryEmbedding] per il gioco [gameId].
     *
     * Se [queryEmbedding] è vuoto (embedding non disponibile) restituisce
     * i primi [topK] chunk per indice (fallback degrado graceful).
     */
    suspend fun retrieve(
        gameId: Long,
        queryEmbedding: FloatArray,
        topK: Int
    ): List<RulebookChunkEntity> {
        val chunks = database.rulebookChunkDao().getByRulebookId(gameId)

        // Fallback: nessun embedding → restituiamo i primi topK per ordine
        if (queryEmbedding.isEmpty()) return chunks.take(topK)

        return chunks
            .filter { it.embeddingBlob != null && it.embeddingBlob!!.isNotEmpty() }
            .map { chunk ->
                val embedding = chunk.embeddingBlob!!.toFloatArray()
                chunk to cosineSimilarity(queryEmbedding, embedding)
            }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }
}
