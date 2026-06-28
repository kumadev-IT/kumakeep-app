package com.kumadev.rulesreader

import com.kumadev.rulesreader.chunker.RulesChunker
import com.kumadev.rulesreader.chunker.SectionDetector
import com.kumadev.rulesreader.model.ExtractedPage
import com.kumadev.rulesreader.model.RulesChunk
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Test di ispezione JVM per la pipeline estrazione+chunking.
 * NON è un test automatizzato con assert — serve per iterare velocemente
 * sui parametri di chunking senza deployare su device.
 *
 * Utilizzo:
 *   ./gradlew :rules-reader:testDebugUnitTest "-Ppdf.path=C:\percorso\regolamento.pdf"
 *
 * Output:
 *   - un breve riepilogo ASCII in console;
 *   - un report HTML leggibile scritto ACCANTO al PDF (stessa cartella), con tabelle
 *     e badge colorati per sectionType. Il percorso e il comando per aprirlo vengono
 *     stampati a fine test. Il file HTML è in UTF-8, quindi immune ai problemi di
 *     encoding della console Windows.
 */
class PipelineInspectorTest {

    @Test
    fun `inspect extraction and chunking pipeline`() {
        val pdfPath = System.getProperty("pdf.path")
        Assume.assumeTrue(
            "Test skipped: imposta -Ppdf.path=C:\\path\\al\\pdf per eseguire",
            !pdfPath.isNullOrBlank()
        )

        val file = File(pdfPath!!)
        require(file.exists()) { "File non trovato: $pdfPath" }

        val pages = extractPages(file)
        val chunker = RulesChunker(SectionDetector())
        val chunks = chunker.chunk(pages)

        // ── Report HTML (leggibile, UTF-8) ────────────────────────────────────
        val report = file.resolveSibling("pipeline-inspector-report.html")
        report.writeText(buildHtml(file, pages, chunks), StandardCharsets.UTF_8)

        // ── Riepilogo ASCII in console (immune a encoding) ─────────────────────
        val emptyPages = pages.count { it.isEmpty }
        val noisyPages = pages.count { it.noiseRatio() > NOISE_THRESHOLD }
        val noisyChunks = chunks.count { it.noiseRatio() > NOISE_THRESHOLD }
        val avgWords = if (chunks.isEmpty()) 0 else chunks.map { it.text.wordCount() }.average().toInt()
        val typeCounts = chunks.groupingBy { it.sectionType ?: "UNKNOWN" }.eachCount()
            .toList().sortedByDescending { it.second }

        println()
        println("=".repeat(60))
        println("  PDF: ${file.name}  (${file.length().toKb()} KB)")
        println("=".repeat(60))
        println("  Pagine totali   : ${pages.size}")
        println("  Pagine vuote    : $emptyPages  (-> OCR su device)")
        println("  Pagine rumorose : $noisyPages  (noise > ${(NOISE_THRESHOLD * 100).toInt()}%)")
        println("  Chunk totali    : ${chunks.size}")
        println("  Chunk rumorosi  : $noisyChunks")
        println("  Parole/chunk avg: $avgWords")
        println("  Chunk per tipo  : " + typeCounts.joinToString(", ") { "${it.first}=${it.second}" })
        println("-".repeat(60))
        println("  REPORT HTML: ${report.absolutePath}")
        println("  Aprilo con:  start \"\" \"${report.absolutePath}\"")
        println("=".repeat(60))
        println()
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

    // ── Generazione report HTML ───────────────────────────────────────────────

    private fun buildHtml(file: File, pages: List<ExtractedPage>, chunks: List<RulesChunk>): String {
        val emptyPages = pages.count { it.isEmpty }
        val noisyPages = pages.count { it.noiseRatio() > NOISE_THRESHOLD }
        val noisyChunks = chunks.count { it.noiseRatio() > NOISE_THRESHOLD }
        val avgWords = if (chunks.isEmpty()) 0 else chunks.map { it.text.wordCount() }.average().toInt()
        val typeCounts = chunks.groupingBy { it.sectionType ?: "UNKNOWN" }.eachCount()
            .toList().sortedByDescending { it.second }

        val sb = StringBuilder()
        sb.append(
            """
            <!DOCTYPE html>
            <html lang="it"><head><meta charset="utf-8">
            <title>Pipeline Inspector — ${esc(file.name)}</title>
            <style>
              :root { color-scheme: light dark; }
              body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; margin: 24px; line-height: 1.4; }
              h1 { font-size: 20px; margin: 0 0 4px; }
              h2 { font-size: 16px; margin: 28px 0 8px; border-bottom: 2px solid #8884; padding-bottom: 4px; }
              .sub { color: #888; font-size: 13px; margin-bottom: 16px; }
              .cards { display: flex; flex-wrap: wrap; gap: 10px; margin: 12px 0; }
              .card { border: 1px solid #8884; border-radius: 10px; padding: 10px 14px; min-width: 120px; }
              .card .n { font-size: 22px; font-weight: 700; }
              .card .l { font-size: 12px; color: #888; }
              table { border-collapse: collapse; width: 100%; font-size: 13px; }
              th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid #8883; vertical-align: top; }
              th { position: sticky; top: 0; background: #7772; backdrop-filter: blur(4px); }
              td.num { text-align: right; font-variant-numeric: tabular-nums; white-space: nowrap; }
              tr.noisy { background: #ff6b6b22; }
              .preview { color: #aaa; font-size: 12px; }
              .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 11px;
                       font-weight: 600; color: #fff; white-space: nowrap; }
              .legend { display: flex; flex-wrap: wrap; gap: 6px; margin: 8px 0 4px; }
              code { background: #7772; padding: 1px 5px; border-radius: 4px; }
            </style></head><body>
            <h1>Pipeline Inspector — ${esc(file.name)}</h1>
            <div class="sub">${file.length().toKb()} KB · report generato dal test JVM (Apache PDFBox, senza OCR)</div>
            <div class="cards">
              ${card(pages.size.toString(), "pagine")}
              ${card(emptyPages.toString(), "vuote → OCR")}
              ${card(noisyPages.toString(), "pagine rumorose")}
              ${card(chunks.size.toString(), "chunk")}
              ${card(noisyChunks.toString(), "chunk rumorosi")}
              ${card(avgWords.toString(), "parole/chunk")}
            </div>
            <div class="legend">${typeCounts.joinToString("") { badge(it.first) + " ×" + it.second + " " }}</div>
            <h2>Chunk (${chunks.size})</h2>
            <table><thead><tr>
              <th>#</th><th>Pag.</th><th>sectionType</th><th class="num">Parole</th>
              <th class="num">Noise</th><th>Anteprima</th>
            </tr></thead><tbody>
            """.trimIndent()
        )
        for (c in chunks) {
            val words = c.text.wordCount()
            val noise = (c.noiseRatio() * 100).toInt()
            val rowClass = if (c.noiseRatio() > NOISE_THRESHOLD) " class=\"noisy\"" else ""
            sb.append(
                "<tr$rowClass><td class=\"num\">${c.index}</td><td class=\"num\">${c.pageNum}</td>" +
                "<td>${badge(c.sectionType ?: "UNKNOWN")}</td>" +
                "<td class=\"num\">$words</td><td class=\"num\">$noise%</td>" +
                "<td class=\"preview\">${esc(c.text.take(220))}${if (c.text.length > 220) "…" else ""}</td></tr>\n"
            )
        }
        sb.append("</tbody></table>\n<h2>Pagine (${pages.size})</h2>\n")
        sb.append(
            "<table><thead><tr><th class=\"num\">Pag.</th><th class=\"num\">Char</th>" +
            "<th class=\"num\">Noise</th><th>Stato</th><th>Anteprima</th></tr></thead><tbody>\n"
        )
        for (p in pages) {
            val noise = (p.noiseRatio() * 100).toInt()
            val rowClass = if (p.noiseRatio() > NOISE_THRESHOLD) " class=\"noisy\"" else ""
            val stato = when {
                p.isEmpty -> "vuota"
                p.noiseRatio() > NOISE_THRESHOLD -> "rumorosa"
                else -> "ok"
            }
            sb.append(
                "<tr$rowClass><td class=\"num\">${p.pageNum}</td><td class=\"num\">${p.rawText.length}</td>" +
                "<td class=\"num\">$noise%</td><td>$stato</td>" +
                "<td class=\"preview\">${esc(p.rawText.take(160).replace('\n', ' '))}</td></tr>\n"
            )
        }
        sb.append("</tbody></table>\n</body></html>")
        return sb.toString()
    }

    private fun card(n: String, label: String) =
        "<div class=\"card\"><div class=\"n\">$n</div><div class=\"l\">${esc(label)}</div></div>"

    private fun badge(type: String): String {
        val color = when (type) {
            "SETUP" -> "#2e7d32"
            "GAMEPLAY" -> "#1565c0"
            "PLAYER_TURN" -> "#6a1b9a"
            "ACTIONS" -> "#ad1457"
            "END_ROUND" -> "#00838f"
            "SCORING" -> "#e65100"
            "SPECIAL_RULES" -> "#4e342e"
            else -> "#607d8b" // UNKNOWN
        }
        return "<span class=\"badge\" style=\"background:$color\">${esc(type)}</span>"
    }

    private fun esc(s: String) = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

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
    private fun RulesChunk.noiseRatio() = text.noiseRatio()

    private fun String.wordCount() = split(Regex("\\s+")).count { it.isNotEmpty() }
    private fun Long.toKb() = this / 1024
}
