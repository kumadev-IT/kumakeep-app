package com.kumadev.rulesreader.llm

/**
 * Interfaccia astratta per l'LLM cloud.
 *
 * Definita in :rules-reader; implementata in :app (GeminiFlashClient).
 * Questo mantiene :rules-reader libero da dipendenze cloud/network.
 */
interface LlmClient {
    /**
     * Invia il prompt all'LLM e restituisce il testo della risposta.
     * Lancia eccezione in caso di errore di rete o risposta non valida.
     */
    suspend fun complete(prompt: String): String
}
