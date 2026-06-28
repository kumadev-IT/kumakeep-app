package com.kumadev.rulesreader.embedding

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Calcola embedding on-device per ogni chunk di testo usando MediaPipe TextEmbedder.
 *
 * Il modello deve essere presente in `assets/rules_reader/sentence_encoder.tflite`.
 * Se il file non è presente, [embed] restituisce [FloatArray(0)] e logga un warning:
 * il processing procede comunque (i chunk vengono salvati senza embedding).
 *
 * Per attivare l'embedding: scaricare il modello Universal Sentence Encoder Lite
 * (https://storage.googleapis.com/mediapipe-models/text_embedder/universal_sentence_encoder/float32/1/universal_sentence_encoder.tflite)
 * e copiarlo in `rules-reader/src/main/assets/rules_reader/sentence_encoder.tflite`.
 */
@Singleton
class EmbeddingEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "EmbeddingEngine"
        private const val MODEL_ASSET_PATH = "rules_reader/sentence_encoder.tflite"
    }

    @Volatile
    private var embedder: TextEmbedder? = null
    private var modelAvailable: Boolean? = null // null = non ancora verificato

    private fun getEmbedder(): TextEmbedder? {
        if (modelAvailable == false) return null
        embedder?.let { return it }

        return try {
            // Verifica che l'asset esista prima di inizializzare MediaPipe
            context.assets.open(MODEL_ASSET_PATH).close()

            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(MODEL_ASSET_PATH)
                .build()
            val options = TextEmbedder.TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .build()
            TextEmbedder.createFromOptions(context, options).also {
                embedder = it
                modelAvailable = true
            }
        } catch (e: Exception) {
            modelAvailable = false
            Log.w(TAG, "Modello sentence encoder non trovato in assets/$MODEL_ASSET_PATH. " +
                "I chunk saranno salvati senza embedding. Dettagli: ${e.message}")
            null
        }
    }

    /**
     * @param text Testo del chunk da embeddare.
     * @return [FloatArray] con il vettore di embedding, oppure [FloatArray(0)] se il modello
     * non è disponibile.
     */
    suspend fun embed(text: String): FloatArray = withContext(Dispatchers.IO) {
        val engine = getEmbedder() ?: return@withContext FloatArray(0)
        try {
            val result = engine.embed(text)
            val floats = result.embeddingResult()
                .embeddings()
                .firstOrNull()
                ?.floatEmbedding()
            when {
                floats == null || floats.isEmpty() -> FloatArray(0)
                else -> FloatArray(floats.size) { i -> floats[i] }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Errore durante embedding: ${e.message}")
            FloatArray(0)
        }
    }

    fun close() {
        embedder?.close()
        embedder = null
        modelAvailable = null
    }
}

// ─── Extension: FloatArray ↔ ByteArray (little-endian, 4 byte per float) ─────

fun FloatArray.toEmbeddingBlob(): ByteArray {
    if (isEmpty()) return ByteArray(0)
    val buffer = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
    forEach { buffer.putFloat(it) }
    return buffer.array()
}

fun ByteArray.toFloatArray(): FloatArray {
    if (isEmpty()) return FloatArray(0)
    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    return FloatArray(size / 4) { buffer.float }
}
