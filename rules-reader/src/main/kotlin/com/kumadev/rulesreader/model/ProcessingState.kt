package com.kumadev.rulesreader.model

sealed interface ProcessingState {
    /** Nessuna elaborazione avviata (o dati non presenti in DB). */
    data object Idle : ProcessingState

    /** Estrazione testo pagina per pagina (PdfBox + OCR fallback). */
    data object Extracting : ProcessingState

    /** Segmentazione in chunk. */
    data class Chunking(val current: Int, val total: Int) : ProcessingState

    /** Calcolo embedding chunk per chunk. */
    data class Embedding(val current: Int, val total: Int) : ProcessingState

    /** Pipeline completata con successo. */
    data object Done : ProcessingState

    /** Errore durante l'elaborazione. */
    data class Error(val message: String) : ProcessingState
}
