package com.kumadev.rulesreader.rag

import com.kumadev.rulesreader.embedding.EmbeddingEngine
import com.kumadev.rulesreader.llm.LlmClient
import com.kumadev.rulesreader.retrieval.ChunkRetriever
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Engine RAG per il chatbot sul regolamento.
 *
 * Flow per ogni messaggio utente:
 * 1. Embedding della query on-device (MediaPipe)
 * 2. Retrieve top-3 chunk più rilevanti (cosine similarity)
 * 3. Costruzione prompt con contesto + history ultimi 6 turni
 * 4. Chiamata LLM (passato come parametro → nessuna dipendenza cloud nel modulo)
 * 5. Restituzione [RagChatMessage] con testo risposta e pagine sorgente
 */
@Singleton
class RagChatEngine @Inject constructor(
    private val chunkRetriever: ChunkRetriever,
    private val embeddingEngine: EmbeddingEngine
) {

    suspend fun chat(
        gameId: Long,
        userMessage: String,
        llmClient: LlmClient,
        history: List<RagChatMessage>
    ): RagChatMessage {
        // 1. Embed query
        val queryEmbedding = embeddingEngine.embed(userMessage)

        // 2. Retrieve top-3 chunk
        val topChunks = chunkRetriever.retrieve(
            gameId = gameId,
            queryEmbedding = queryEmbedding,
            topK = 3
        )

        // 3. Costruisci contesto e lista pagine sorgente
        val context = topChunks.joinToString("\n\n") { "[p.${it.pageNum}]\n${it.text}" }
        val sourcePages = topChunks.map { it.pageNum }.distinct().sorted()

        // 4. Costruisci history (ultimi 6 messaggi per non sforare il contesto)
        val historyText = history.takeLast(6).joinToString("\n") { msg ->
            val role = if (msg.role == RagChatMessage.Role.User) "Utente" else "Assistente"
            "$role: ${msg.text}"
        }

        // 5. Prompt
        val prompt = buildPrompt(context, historyText, userMessage)

        // 6. Chiama LLM
        val responseText = try {
            llmClient.complete(prompt)
        } catch (e: Exception) {
            "Errore di connessione: ${e.message}"
        }

        return RagChatMessage(
            role = RagChatMessage.Role.Assistant,
            text = responseText.trim(),
            sourcePageNums = sourcePages
        )
    }

    private fun buildPrompt(
        context: String,
        historyText: String,
        userMessage: String
    ): String = buildString {
        appendLine("Sei un assistente per giochi da tavolo. Rispondi SOLO basandoti sul testo del regolamento fornito qui sotto.")
        appendLine()
        appendLine("REGOLE:")
        appendLine("- Rispondi sempre in italiano")
        appendLine("- Se l'informazione non è nel testo, rispondi ESATTAMENTE: \"Purtroppo non so come rispondere alla domanda\"")
        appendLine("- Cita sempre il numero di pagina usando la notazione (p.X)")
        appendLine("- Sii conciso e diretto")
        appendLine()
        appendLine("TESTO DEL REGOLAMENTO (sezioni più rilevanti):")
        appendLine("---")
        appendLine(context)
        appendLine("---")
        if (historyText.isNotBlank()) {
            appendLine()
            appendLine("CRONOLOGIA CONVERSAZIONE:")
            appendLine(historyText)
        }
        appendLine()
        append("DOMANDA: $userMessage")
    }
}
