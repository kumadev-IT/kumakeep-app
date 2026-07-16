package com.kumadev.rulesreader.ludomancer

import java.io.File

/**
 * Contratto verso il backend **Ludomancer** (motore AI esperto di giochi da tavolo,
 * sviluppato come progetto separato).
 *
 * Definito in :rules-reader; l'implementazione REALE (Retrofit/OkHttp) vivrà in :app,
 * esattamente come [com.kumadev.rulesreader.llm.LlmClient] → GeminiFlashClient.
 * Finché il backend non è pronto è attivo `MockLudomancerClient` (risponde "coming soon").
 * Passare al client reale sarà un cambio di UN binding Hilt.
 *
 * ## Flusso d'uso (lazy / idempotente)
 * KumaKeep è l'unico a possedere il PDF del regolamento (importato dall'utente).
 * Per evitare ingestioni ripetute:
 * 1. [status] — Ludomancer conosce già questo `bggId`?
 * 2. se `NOT_INGESTED` → [ingest] con il PDF; poi si attende `READY` via [status]
 * 3. [ask] — l'utente interroga Ludomancer
 * 4. [feedback] — le correzioni alimentano l'apprendimento del backend
 *
 * ## Mappa endpoint REST
 * ```
 * status()   -> GET  /games/{bggId}/status
 * ingest()   -> POST /games/{bggId}/ingest      (multipart: rulebook PDF)
 * ask()      -> POST /games/{bggId}/ask         { question, history }
 * feedback() -> POST /games/{bggId}/feedback    { questionId, correction }
 * ```
 * `bggId` è l'identificatore BoardGameGeek già usato in KumaKeep: chiave stabile
 * e condivisibile, così Ludomancer può deduplicare l'ingestione tra client diversi.
 */
interface LudomancerClient {

    /** Ludomancer conosce già questo gioco? Chiamare PRIMA di [ingest]. */
    suspend fun status(bggId: Long): LudomancerStatus

    /**
     * Invia il PDF del regolamento per l'elaborazione.
     * Da chiamare solo se [status] ha restituito [LudomancerState.NOT_INGESTED].
     */
    suspend fun ingest(bggId: Long, rulebookPdf: File): IngestAck

    /**
     * L'utente chiede a Ludomancer. Cuore dell'interazione.
     * @param history turni precedenti (contesto conversazionale), opzionale.
     */
    suspend fun ask(
        bggId: Long,
        question: String,
        history: List<LudomancerTurn> = emptyList()
    ): LudomancerAnswer

    /**
     * Correzione dell'utente a una risposta → segnale di apprendimento per il backend.
     * @param questionId id restituito insieme alla risposta (per ora ignorato dal mock).
     */
    suspend fun feedback(bggId: Long, questionId: String, correction: String)
}
