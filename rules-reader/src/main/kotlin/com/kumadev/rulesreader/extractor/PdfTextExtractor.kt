package com.kumadev.rulesreader.extractor

import com.kumadev.rulesreader.model.ExtractedPage
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Estrae il testo selezionabile da un PDF pagina per pagina usando PdfBox-Android.
 *
 * Le pagine con testo vuoto o insufficiente (< [MIN_TEXT_LENGTH] caratteri) sono
 * marcate come "vuote" e dovranno essere processate dall'[OcrFallbackExtractor].
 */
@Singleton
class PdfTextExtractor @Inject constructor() {

    companion object {
        /** Soglia minima: pagine con meno caratteri sono trattate come PDF-immagine. */
        private const val MIN_TEXT_LENGTH = 30
    }

    /**
     * @param filePath percorso assoluto del file PDF in filesDir.
     * @return lista ordinata di [ExtractedPage], una per pagina.
     */
    suspend fun extract(filePath: String): List<ExtractedPage> = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.isReady() // inizializzazione lazy di PdfBox (richiede ApplicationContext
        // iniettato nel modulo — PDFBoxResourceLoader.init() va chiamato in RulesReaderModule)

        val stripper = PDFTextStripper()
        PDDocument.load(java.io.File(filePath)).use { document ->
            val totalPages = document.numberOfPages
            (1..totalPages).map { pageNum ->
                stripper.startPage = pageNum
                stripper.endPage = pageNum
                val text = stripper.getText(document).trim()
                ExtractedPage(
                    pageNum = pageNum,
                    rawText = if (text.length >= MIN_TEXT_LENGTH) text else ""
                )
            }
        }
    }
}
