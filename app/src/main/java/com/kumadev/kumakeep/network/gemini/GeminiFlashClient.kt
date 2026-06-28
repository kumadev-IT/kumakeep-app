package com.kumadev.kumakeep.network.gemini

import android.util.Log
import com.kumadev.rulesreader.llm.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Implementazione di [LlmClient] basata su Gemini 2.0 Flash.
 *
 * Usa OkHttp direttamente (client separato da quello BGG) per evitare
 * interferenze con l'interceptor XML. La risposta JSON viene parsata
 * con [org.json] (disponibile su Android senza dipendenze extra).
 *
 * L'API key viene iniettata da BuildConfig tramite [GeminiModule]:
 * aggiungere `GEMINI_API_KEY=la_tua_chiave` in `local.properties`.
 */
@Singleton
class GeminiFlashClient @Inject constructor(
    @Named("gemini_api_key") private val apiKey: String
) : LlmClient {

    companion object {
        private const val TAG = "GeminiFlashClient"
        private const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val MAX_RETRIES = 3
        private const val RETRY_BASE_DELAY_MS = 5_000L // 5s, 10s, 20s
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun complete(prompt: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException(
                "GEMINI_API_KEY non configurata. Aggiungila in local.properties."
            )
        }

        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    })
                })
            })
        }.toString()

        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            if (attempt > 0) {
                val delayMs = RETRY_BASE_DELAY_MS * (1L shl (attempt - 1)) // 5s, 10s, 20s
                Log.w(TAG, "Retry $attempt/$MAX_RETRIES dopo ${delayMs}ms")
                delay(delayMs)
            }

            val request = Request.Builder()
                .url("$ENDPOINT?key=$apiKey")
                .post(requestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
                ?: throw IllegalStateException("Risposta vuota da Gemini API")

            if (response.isSuccessful) {
                val json = JSONObject(body)
                return@withContext json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }

            Log.e(TAG, "Gemini error ${response.code} (attempt ${attempt + 1}): $body")
            lastException = IllegalStateException("Gemini API error ${response.code}: ${response.message}")

            // Non ritentare se non è un errore transitorio
            if (response.code != 429 && response.code != 503) {
                throw lastException!!
            }
        }

        throw lastException ?: IllegalStateException("Gemini API: tutti i retry falliti")
    }
}
