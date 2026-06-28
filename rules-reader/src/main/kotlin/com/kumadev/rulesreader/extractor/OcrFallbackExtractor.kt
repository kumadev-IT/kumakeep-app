package com.kumadev.rulesreader.extractor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Estrae il testo da pagine PDF senza testo selezionabile (PDF-immagine) usando
 * ML Kit Text Recognition (bundled, completamente offline).
 *
 * Usa [android.graphics.pdf.PdfRenderer] nativo per renderizzare la pagina in bitmap
 * prima di passarla al riconoscitore OCR.
 */
@Singleton
class OcrFallbackExtractor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Esegue OCR su tutte le pagine indicate da [emptyPageNums] nel file [filePath].
     *
     * @param filePath percorso assoluto del PDF.
     * @param emptyPageNums set di numeri di pagina (1-based) da processare.
     * @return mappa pageNum → testo OCR estratto.
     */
    suspend fun extractPages(
        filePath: String,
        emptyPageNums: Set<Int>
    ): Map<Int, String> = withContext(Dispatchers.IO) {
        if (emptyPageNums.isEmpty()) return@withContext emptyMap()

        val result = mutableMapOf<Int, String>()
        val fd = ParcelFileDescriptor.open(
            java.io.File(filePath),
            ParcelFileDescriptor.MODE_READ_ONLY
        )
        PdfRenderer(fd).use { renderer ->
            emptyPageNums.forEach { pageNum ->
                val pageIndex = pageNum - 1
                if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@forEach
                renderer.openPage(pageIndex).use { page ->
                    val bitmap = Bitmap.createBitmap(
                        page.width * 2, // 2x risoluzione per migliore qualità OCR
                        page.height * 2,
                        Bitmap.Config.ARGB_8888
                    )
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    val text = recognizeText(bitmap)
                    bitmap.recycle()
                    if (text.isNotBlank()) result[pageNum] = text
                }
            }
        }
        result
    }

    private suspend fun recognizeText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result -> cont.resume(result.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }
}
