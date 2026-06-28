package com.kumadev.rulesreader

import com.kumadev.rulesreader.chunker.RulesChunker
import com.kumadev.rulesreader.model.ExtractedPage
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assume
import org.junit.Test
import java.io.File

/**
 * Test di ispezione JVM per la pipeline estrazione+chunking.
 * NON è un test automatizzato con assert — serve per iterare velocemente
 * sui parametri di chunking senza deployare su device.
 *
 * Utilizzo:
 *   ./gradlew :rules-reader:test --tests "*.PipelineInspectorTest" \
 *     -Dpdf.path=/percorso/locale/regolamento.pdf
 *
 * Per copiare il PDF dal device:
 *   adb pull /data/user/0/com.kumadev.kumakeep.debug/files/rulebooks/<gameId>.pdf ./test.pdf
 *
 * Output: statistiche pagine + chunk su stdout, con indicatori di qualità testo.
 */
class PipelineInspectorTest {

    @Test
    fun `inspect extraction and chunking pipeline`() {
        val pdfPath = System.getProperty("pdf.path")
        Assume.assumeTrue(
            "Test skipped: imposta -Dpdf.path=/path/al/pdf per eseguire",
            pdfPath != null
        )

        val file = File(pdfPath!!)
        require(file.exists()) { "File non trovato: $pdfPath" }

        println("\n" + "═".repeat(70))
        println("PDF: ${file.name}  (${file.length().toKb()} KB)")
        println("═".repeat(70))

        // ── Estrazione testo ───────────────────────────────────────────────────
        val pages = extractPages(file)
        printPagesReport(pages)

        // ── Chunking ───────────────────────────────────────────────────────────
        val chunker = RulesChunker()
        val chunks = chunker.chunk(pages)
        printChunksReport(chunks)

        // ── Riepilogo ─────────────────────────────────────────────────────────
        val noisyPages = pages.count { it.noiseRatio() > NOISE_THRESHOLD }
        val noisyChunks = chunks.count { it.noiseRatio() > NOISE_THRESHOLD }
        val emptyPages = pages.count { it.isEmpty }

        println("\n${"─".repeat(70)}")
        println("RIEPILOGO")
        println("  Pagine totali   : ${pages.size}")
        println("  Pagine vuote    : $emptyPages  ← andranno all'OCR su device")
        println("  Pagine rumorose : $noisyPages  (noise > ${(NOISE_THRESHOLD * 100).toInt()}%)")
        println("  Chunk totali    : ${chunks.size}")
        println("  Chunk rumorosi  : $noisyChunks")
        println("  Parole/chunk avg: ${chunks.map { it.text.wordCount() }.average().toInt()}")
        println("─".repeat(70) + "\n")
    }

    // ── Estrazione con Apache PDFBox (JVM) ────────────────────────────────────

    private fun extractPages(file: File): List<ExtractedPage> {
        val stripper = PDFTextStripper()
        return Loader.loadPDF(file).use { doc ->
            (1..doc.numberOfPages).map { pageNum ->
                stripper.startPage = pageNum
                stripper.endPage = pageNum
                val text = stripper.getText(doc).trim()
                ExtractedPage(
                    pageNum = pageNum,
                    rawText = if (text.length >= MIN_TEXT_LENGTH) text else ""
                )
            }
        }
    }

    // ── Report pagine ─────────────────────────────────────────────────────────

    private fun printPagesReport(pages: List<ExtractedPage>) {
        println("\nPAGINE (${pages.size})")
        println("─".repeat(70))
        pages.forEach { page ->
            val noise = page.noiseRatio()
            val noiseTag = if (noise > NOISE_THRESHOLD) " ⚠ RUMOROSA" else ""
            val emptyTag = if (page.isEmpty) " ◌ VUOTA" else ""
            println(
                "p.${page.pageNum.toString().padEnd(3)}  " +
                "${page.rawText.length.toString().padStart(6)} chars  " +
                "noise=${(noise * 100).toInt().toString().padStart(3)}%" +
                noiseTag + emptyTag
            )
            if (page.rawText.isNotBlank() && noise <= NOISE_THRESHOLD) {
                // Preview prime 120 chars per pagine pulite
                println("       ↳ ${page.rawText.take(120).replace('\n', ' ')}")
            } else if (page.rawText.isNotBlank()) {
                // Per pagine rumorose mostra il pattern che causa il problema
                println("       ↳ ${page.rawText.take(120).replace('\n', ' ')}")
            }
        }
    }

    // ── Report chunk ──────────────────────────────────────────────────────────

    private fun printChunksReport(chunks: List<com.kumadev.rulesreader.model.RulesChunk>) {
        println("\nCHUNK (${chunks.size})  [size=${RulesChunker.CHUNK_WORDS}w, overlap=${RulesChunker.OVERLAP_WORDS}w]")
        println("─".repeat(70))
        chunks.forEach { chunk ->
            val words = chunk.text.wordCount()
            val noise = chunk.noiseRatio()
            val noiseTag = if (noise > NOISE_THRESHOLD) " ⚠" else ""
            println(
                "#${chunk.index.toString().padEnd(3)}  p.${chunk.pageNum.toString().padEnd(3)}  " +
                "${words.toString().padStart(4)} parole  " +
                "noise=${(noise * 100).toInt().toString().padStart(3)}%$noiseTag"
            )
            println("       ↳ ${chunk.text.take(150).replace('\n', ' ')}…")
            println()
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    companion object {
        private const val MIN_TEXT_LENGTH = 30
        /** Soglia: se più del 40% delle parole ha ≤2 caratteri → testo rumoroso */
        private const val NOISE_THRESHOLD = 0.40f
    }

    /** Rapporto parole corte (≤2 chars) sul totale — indicatore di OCR/font garbage */
    private fun String.noiseRatio(): Float {
        val words = split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return 0f
        val shortWords = words.count { it.length <= 2 }
        return shortWords.toFloat() / words.size
    }

    private fun ExtractedPage.noiseRatio() = rawText.noiseRatio()
    private fun com.kumadev.rulesreader.model.RulesChunk.noiseRatio() = text.noiseRatio()

    private fun String.wordCount() = split(Regex("\\s+")).count { it.isNotEmpty() }
    private fun Long.toKb() = this / 1024
}
