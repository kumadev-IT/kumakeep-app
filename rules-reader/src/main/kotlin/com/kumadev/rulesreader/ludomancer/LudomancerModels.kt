package com.kumadev.rulesreader.ludomancer

/**
 * Modelli del contratto REST verso il backend **Ludomancer** (progetto separato).
 *
 * Sono volutamente disaccoppiati dai modelli interni di :rules-reader
 * (RagChatMessage, RulesChunk, ...): questo è il confine pubblico tra
 * KumaKeep (client) e Ludomancer (servizio). Cambiando l'implementazione
 * da mock a REST reale, questi tipi non cambiano.
 */

/** Stato di elaborazione di un gioco lato Ludomancer — `GET /games/{bggId}/status`. */
enum class LudomancerState {
    /** Ludomancer non ha ancora questo gioco: KumaKeep deve inviare il PDF. */
    NOT_INGESTED,
    PARSING,
    INDEXING,
    /** Pronto a rispondere alle domande. */
    READY,
    ERROR
}

/** Risposta di `GET /games/{bggId}/status`. */
data class LudomancerStatus(
    val state: LudomancerState,
    /** Avanzamento 0f..1f durante parsing/indexing. */
    val progress: Float = 0f,
    val detail: String? = null
)

/** Ack dell'avvio ingestione — `POST /games/{bggId}/ingest` (multipart PDF). */
data class IngestAck(
    val jobId: String,
    val state: LudomancerState
)

/** Citazione a supporto di una risposta: sempre ancorata al regolamento. */
data class LudomancerCitation(
    val page: Int,
    val section: String? = null,
    val quote: String
)

/** Turno di conversazione passato come contesto ad [LudomancerClient.ask]. */
data class LudomancerTurn(
    val role: Role,
    val text: String
) {
    enum class Role { User, Assistant }
}

/** Risposta a una domanda — `POST /games/{bggId}/ask`. */
data class LudomancerAnswer(
    val answer: String,
    val citations: List<LudomancerCitation> = emptyList(),
    /** Confidenza 0f..1f, se il backend la espone. */
    val confidence: Float? = null,
    /**
     * true finché la risposta arriva da un mock/placeholder (backend non ancora pronto).
     * La UI può usarlo per mostrare un badge "coming soon" invece di trattarlo come regola.
     */
    val placeholder: Boolean = false
)
