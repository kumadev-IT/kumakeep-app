package com.kumadev.rulesreader.rag

/**
 * Singolo messaggio della chat RAG.
 *
 * La history NON viene salvata in Room — vive solo in-memory nel ViewModel.
 */
data class RagChatMessage(
    val role: Role,
    val text: String,
    /** Pagine sorgente dei chunk usati per la risposta (solo per messaggi dell'Assistant). */
    val sourcePageNums: List<Int> = emptyList()
) {
    enum class Role { User, Assistant }
}
