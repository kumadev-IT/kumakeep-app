package com.kumadev.rulesreader.generator

import android.util.Log
import com.kumadev.rulesreader.BuildConfig
import com.kumadev.rulesreader.db.RulesReaderDatabase
import com.kumadev.rulesreader.db.entity.GeneratedScreenEntity
import com.kumadev.rulesreader.llm.LlmClient
import org.json.JSONArray
import org.json.JSONException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Genera le schermate di apprendimento per un regolamento.
 *
 * Flow:
 * 1. Carica i chunk puliti (noise ratio ≤ 40%) dal DB
 * 2. Costruisce un prompt strutturato per l'LLM
 * 3. Chiama [LlmClient.complete] (implementato in :app — es. Gemini Flash)
 * 4. Parsa la risposta JSON
 * 5. Salva le schermate in Room
 *
 * La generazione è una-tantum: se le schermate esistono già, non vengono rigenerate
 * (gestito dal chiamante tramite [RulesReaderDatabase.generatedScreenDao]).
 */
@Singleton
class LearningScreensGenerator @Inject constructor(
    private val database: RulesReaderDatabase
) {

    companion object {
        private const val TAG = "LearningScreensGenerator"
    }

    suspend fun generate(gameId: Long, llmClient: LlmClient) {
        // 1. Carica e filtra chunk (logica condivisa con il tool di ispezione JVM)
        val allChunks = database.rulebookChunkDao().getByRulebookId(gameId)
        val inputs = allChunks.map {
            LearningScreensPrompt.ChunkInput(it.pageNum, it.text, it.sectionType)
        }
        val cleanChunks = LearningScreensPrompt.selectCleanChunks(inputs)

        Log.d(TAG, "gameId=$gameId: ${allChunks.size} chunk totali, ${cleanChunks.size} puliti")

        if (cleanChunks.isEmpty()) {
            Log.w(TAG, "gameId=$gameId: nessun chunk pulito — impossibile generare schermate")
            return
        }

        // 2. Costruisci testo contesto (con numero pagina) e prompt
        val contextText = LearningScreensPrompt.buildContext(cleanChunks)

        // 3. Prompt
        val prompt = LearningScreensPrompt.buildPrompt(contextText)

        // 4. Chiama LLM
        Log.d(TAG, "gameId=$gameId: invio prompt a LLM (${prompt.length} chars)")
        val rawResponse = llmClient.complete(prompt)
        Log.d(TAG, "gameId=$gameId: risposta ricevuta (${rawResponse.length} chars)")

        // 5. Parsa
        val screens = parseScreens(rawResponse, gameId)
        if (screens.isEmpty()) {
            Log.w(TAG, "gameId=$gameId: nessuna schermata parseable")
            return
        }

        // 6. Salva in Room (sostituisce generazione precedente)
        database.generatedScreenDao().deleteByRulebookId(gameId)
        database.generatedScreenDao().insertAll(screens)
        Log.d(TAG, "gameId=$gameId: ${screens.size} schermate salvate")
    }

    private fun parseScreens(raw: String, gameId: Long): List<GeneratedScreenEntity> {
        val jsonStr = extractJsonArray(raw)
        return try {
            val arr = JSONArray(jsonStr)
            val now = System.currentTimeMillis()
            (0 until arr.length()).mapNotNull { i ->
                try {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("title", "").trim()
                    val body = obj.optString("body", "").trim()
                    if (title.isBlank() || body.isBlank()) return@mapNotNull null

                    val pageNumsArr = obj.optJSONArray("sourcePageNums")
                    val pageNums = (0 until (pageNumsArr?.length() ?: 0))
                        .map { j -> pageNumsArr!!.optInt(j, 0) }
                        .filter { it > 0 }
                        .joinToString(",")

                    GeneratedScreenEntity(
                        rulebookId = gameId,
                        screenIndex = i,
                        title = title,
                        body = body,
                        sourcePageNums = pageNums,
                        generatedAt = now,
                        rulesReaderVersion = BuildConfig.RULES_READER_VERSION
                    )
                } catch (e: JSONException) {
                    Log.w(TAG, "Errore parsing schermata $i: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore parsing JSON: ${e.message}\nRaw: ${raw.take(500)}")
            emptyList()
        }
    }

    /** Estrae il primo JSON array dalla risposta, rimuovendo eventuali blocchi markdown. */
    private fun extractJsonArray(raw: String): String {
        val stripped = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = stripped.indexOf('[')
        val end = stripped.lastIndexOf(']')
        return if (start >= 0 && end > start) stripped.substring(start, end + 1) else stripped
    }
}
