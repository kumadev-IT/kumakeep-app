package com.kumadev.rulesreader

import android.util.Log
import com.kumadev.rulesreader.BuildConfig
import com.kumadev.rulesreader.chunker.RulesChunker
import com.kumadev.rulesreader.db.RulesReaderDatabase
import com.kumadev.rulesreader.db.entity.RulebookChunkEntity
import com.kumadev.rulesreader.embedding.EmbeddingEngine
import com.kumadev.rulesreader.embedding.toEmbeddingBlob
import com.kumadev.rulesreader.extractor.OcrFallbackExtractor
import com.kumadev.rulesreader.extractor.PdfTextExtractor
import com.kumadev.rulesreader.model.ExtractedPage
import com.kumadev.rulesreader.model.ProcessingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Entry point pubblico del modulo :rules-reader.
 *
 * Coordina l'intera pipeline per un singolo regolamento:
 * 1. Estrazione testo (PdfBox + OCR fallback ML Kit)
 * 2. Chunking (sliding window)
 * 3. Embedding on-device (MediaPipe)
 *
 * Lo stato di avanzamento è esposto tramite [getProcessingState] come [Flow].
 * Tutto il processing avviene su [Dispatchers.IO].
 */
@Singleton
class RulesReader @Inject constructor(
    private val database: RulesReaderDatabase,
    private val pdfTextExtractor: PdfTextExtractor,
    private val ocrFallbackExtractor: OcrFallbackExtractor,
    private val chunker: RulesChunker,
    private val embeddingEngine: EmbeddingEngine
) {
    companion object {
        private const val TAG = "RulesReader"
    }

    /**
     * Scope singleton per i check di inizializzazione dello stato dal DB.
     * Separato da viewModelScope per sopravvivere alle rotazioni.
     */
    private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Mappa gameId → StateFlow<ProcessingState> */
    private val stateFlows = ConcurrentHashMap<Long, MutableStateFlow<ProcessingState>>()

    /**
     * Restituisce il [Flow] di [ProcessingState] per il gioco indicato.
     * Se ci sono già dati in DB, inizializza lo stato a [ProcessingState.Done].
     */
    fun getProcessingState(gameId: Long): Flow<ProcessingState> {
        return stateFlows.getOrPut(gameId) {
            MutableStateFlow<ProcessingState>(ProcessingState.Idle).also { flow ->
                // Inizializza dallo stato persistito in DB
                moduleScope.launch {
                    if (database.rulebookChunkDao().countByRulebookId(gameId) > 0) {
                        flow.value = ProcessingState.Done
                    }
                }
            }
        }
    }

    /**
     * Esegue la pipeline completa per il regolamento del gioco indicato.
     *
     * Non fa nulla se il processing è già in corso o completato.
     * Per forzare una ri-elaborazione, cancellare prima i dati dal DB.
     *
     * @param gameId ID del gioco (= rulebookId nelle entity del modulo).
     * @param filePath Percorso assoluto del PDF in filesDir.
     */
    suspend fun process(gameId: Long, filePath: String) {
        val stateFlow = stateFlows.getOrPut(gameId) { MutableStateFlow(ProcessingState.Idle) }

        // Protezione da doppio avvio
        val current = stateFlow.value
        if (current is ProcessingState.Done ||
            current is ProcessingState.Extracting ||
            current is ProcessingState.Chunking ||
            current is ProcessingState.Embedding) {
            Log.d(TAG, "gameId=$gameId: pipeline già avviata o completata, skip.")
            return
        }

        try {
            // ── Step 1: Estrazione testo ───────────────────────────────────────
            stateFlow.value = ProcessingState.Extracting
            Log.d(TAG, "gameId=$gameId: avvio estrazione testo da $filePath")

            val pdfPages = pdfTextExtractor.extract(filePath)

            // Identifica pagine vuote (PDF-immagine) → OCR fallback
            val emptyPageNums = pdfPages.filter { it.isEmpty }.map { it.pageNum }.toSet()
            val ocrResults: Map<Int, String> = if (emptyPageNums.isNotEmpty()) {
                Log.d(TAG, "gameId=$gameId: OCR fallback su ${emptyPageNums.size} pagine vuote")
                ocrFallbackExtractor.extractPages(filePath, emptyPageNums)
            } else emptyMap()

            // Unisce i risultati: le pagine OCR sostituiscono quelle vuote
            val mergedPages: List<ExtractedPage> = pdfPages.map { page ->
                if (page.isEmpty) {
                    page.copy(rawText = ocrResults[page.pageNum] ?: "")
                } else page
            }

            // Salva nel DB (pulizia prima di inserimento per ri-elaborazione sicura)
            database.extractedPageDao().deleteByRulebookId(gameId)
            database.extractedPageDao().insertAll(mergedPages.map { it.toEntity(gameId) })
            Log.d(TAG, "gameId=$gameId: ${mergedPages.size} pagine salvate in DB")

            // ── Step 2: Chunking ───────────────────────────────────────────────
            val chunks = chunker.chunk(mergedPages)
            stateFlow.value = ProcessingState.Chunking(0, chunks.size)
            Log.d(TAG, "gameId=$gameId: ${chunks.size} chunk generati")

            // Pulizia chunk precedenti
            database.rulebookChunkDao().deleteByRulebookId(gameId)

            // ── Step 3: Embedding ──────────────────────────────────────────────
            chunks.forEachIndexed { index, chunk ->
                stateFlow.value = ProcessingState.Embedding(index, chunks.size)

                val embedding = embeddingEngine.embed(chunk.text)
                val blob = embedding.toEmbeddingBlob().takeIf { it.isNotEmpty() }

                database.rulebookChunkDao().insert(
                    RulebookChunkEntity(
                        rulebookId = gameId,
                        chunkIndex = chunk.index,
                        pageNum = chunk.pageNum,
                        text = chunk.text,
                        embeddingBlob = blob,
                        rulesReaderVersion = BuildConfig.RULES_READER_VERSION
                    )
                )
            }

            stateFlow.value = ProcessingState.Done
            Log.d(TAG, "gameId=$gameId: pipeline completata (${chunks.size} chunk)")

        } catch (e: Exception) {
            val msg = e.message ?: "Errore sconosciuto"
            Log.e(TAG, "gameId=$gameId: errore pipeline: $msg", e)
            stateFlow.value = ProcessingState.Error(msg)
        }
    }

    /**
     * Resetta lo stato per il gioco indicato e cancella i dati dal DB,
     * permettendo una ri-elaborazione da zero.
     */
    suspend fun reset(gameId: Long) {
        database.extractedPageDao().deleteByRulebookId(gameId)
        database.rulebookChunkDao().deleteByRulebookId(gameId)
        stateFlows[gameId]?.value = ProcessingState.Idle
        Log.d(TAG, "gameId=$gameId: stato resettato")
    }
}
