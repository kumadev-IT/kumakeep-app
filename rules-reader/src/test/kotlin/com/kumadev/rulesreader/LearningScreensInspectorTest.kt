package com.kumadev.rulesreader

import com.kumadev.rulesreader.chunker.RulesChunker
import com.kumadev.rulesreader.chunker.SectionDetector
import com.kumadev.rulesreader.generator.LearningScreensPrompt
import com.kumadev.rulesreader.generator.LearningScreensPrompt.ChunkInput
import com.kumadev.rulesreader.model.ExtractedPage
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assume
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Tool di ispezione JVM per le SCHERMATE DI APPRENDIMENTO.
 *
 * Permette di iterare su PC senza device: usa la logica condivisa [LearningScreensPrompt]
 * (identica alla produzione) per mostrare
 *   - quali chunk superano il filtro rumore;
 *   - il contesto e il prompt ESATTI inviati all'LLM;
 *   - la copertura per sectionType e cosa la troncatura a 10.000 char taglia fuori;
 *   - (opzionale) le schermate realmente generate da Gemini.
 *
 * Uso (solo input, nessun costo LLM):
 *   ./gradlew :rules-reader:testDebugUnitTest \
 *     --tests "*LearningScreensInspectorTest" "-Ppdf.path=C:\path\regolamento.pdf"
 *
 * Uso con LLM reale (consuma quota):
 *   ... "-Ppdf.path=..." "-Pgemini.key=LA_TUA_API_KEY" ["-Pgemini.model=gemini-2.5-flash"]
 *
 * Output: report HTML `learning-screens-report.html` accanto al PDF (UTF-8) + riepilogo console.
 */
class LearningScreensInspectorTest {

    @Test
    fun `inspect learning screens generation`() {
        val pdfPath = System.getProperty("pdf.path")
        Assume.assumeTrue(
            "Test skipped: imposta -Ppdf.path=C:\\path\\al\\pdf per eseguire",
            !pdfPath.isNullOrBlank()
        )
        val file = File(pdfPath!!)
        require(file.exists()) { "File non trovato: $pdfPath" }

        // 1. Estrazione + chunking (stessa pipeline dell'inspector chunk)
        val pages = extractPages(file)
        val chunks = RulesChunker(SectionDetector()).chunk(pages)
        val inputs = chunks.map { ChunkInput(it.pageNum, it.text, it.sectionType) }

        // 2. Filtro rumore + contesto + prompt (logica condivisa con la produzione)
        val clean = LearningScreensPrompt.selectCleanChunks(inputs)
        val context = LearningScreensPrompt.buildContext(clean)
        val prompt = LearningScreensPrompt.buildPrompt(context)

        // 3. Copertura del contesto: quali chunk entrano nel limite 10k, quali no
        val coverage = computeCoverage(clean)
        val fullJoinLen = clean.joinToString("\n\n") { "[p.${it.pageNum}]\n${it.text}" }.length
        val truncated = fullJoinLen > LearningScreensPrompt.CONTEXT_CHAR_LIMIT

        // 4. LLM opzionale
        val apiKey = System.getProperty("gemini.key").orEmpty()
        val model = System.getProperty("gemini.model").ifBlankOrNull("gemini-2.5-flash")
        var rawResponse: String? = null
        var screens: List<Screen> = emptyList()
        var llmError: String? = null
        if (apiKey.isNotBlank()) {
            try {
                rawResponse = callGemini(apiKey, model, prompt)
                screens = parseScreens(rawResponse)
            } catch (e: Exception) {
                llmError = e.message ?: e.toString()
            }
        }

        // 5. Report HTML
        val report = file.resolveSibling("learning-screens-report.html")
        report.writeText(
            buildHtml(file, chunks.size, clean.size, context, prompt, coverage, truncated,
                model, apiKey.isNotBlank(), rawResponse, screens, llmError),
            StandardCharsets.UTF_8
        )

        // 6. Console
        val droppedTypes = coverage.filter { it.outCount > 0 }
        println()
        println("=".repeat(60))
        println("  LEARNING SCREENS — ${file.name}")
        println("=".repeat(60))
        println("  Chunk totali       : ${chunks.size}")
        println("  Chunk puliti        : ${clean.size}  (filtro rumore generator)")
        println("  Contesto            : ${context.length} / ${LearningScreensPrompt.CONTEXT_CHAR_LIMIT} char" +
            if (truncated) "  <-- TRONCATO (join reale=${fullJoinLen})" else "")
        println("  Prompt              : ${prompt.length} char")
        if (droppedTypes.isNotEmpty()) {
            println("  Sezioni tagliate    : " +
                droppedTypes.joinToString(", ") { "${it.type}(${it.outCount})" })
        }
        when {
            apiKey.isBlank() -> println("  LLM                 : non chiamato (nessuna -Pgemini.key)")
            llmError != null -> println("  LLM                 : ERRORE — $llmError")
            else -> println("  Schermate generate  : ${screens.size}  (${screens.joinToString(", ") { it.title }})")
        }
        println("-".repeat(60))
        println("  REPORT: ${report.absolutePath}")
        println("  Apri:   start \"\" \"${report.absolutePath}\"")
        println("=".repeat(60))
        println()
    }

    // ── Copertura contesto ────────────────────────────────────────────────────

    private data class TypeCoverage(val type: String, val total: Int, val inCount: Int, val outCount: Int)

    /** Riproduce la troncatura a 10k per sapere quali chunk entrano davvero nel contesto. */
    private fun computeCoverage(clean: List<ChunkInput>): List<TypeCoverage> {
        val limit = LearningScreensPrompt.CONTEXT_CHAR_LIMIT
        var cum = 0
        val inByType = HashMap<String, Int>()
        val outByType = HashMap<String, Int>()
        val totByType = HashMap<String, Int>()
        for ((i, c) in clean.withIndex()) {
            val type = c.sectionType ?: "UNKNOWN"
            totByType[type] = (totByType[type] ?: 0) + 1
            val piece = "[p.${c.pageNum}]\n${c.text}"
            val add = if (i == 0) piece.length else 2 + piece.length // "\n\n"
            if (cum < limit) {
                inByType[type] = (inByType[type] ?: 0) + 1
                cum += add
            } else {
                outByType[type] = (outByType[type] ?: 0) + 1
            }
        }
        return totByType.keys.map {
            TypeCoverage(it, totByType[it] ?: 0, inByType[it] ?: 0, outByType[it] ?: 0)
        }.sortedByDescending { it.total }
    }

    // ── Estrazione PDFBox (JVM) ───────────────────────────────────────────────

    private fun extractPages(file: File): List<ExtractedPage> {
        val stripper = PDFTextStripper()
        return Loader.loadPDF(file).use { doc ->
            (1..doc.numberOfPages).map { pageNum ->
                stripper.startPage = pageNum
                stripper.endPage = pageNum
                val text = stripper.getText(doc).trim()
                ExtractedPage(pageNum = pageNum, rawText = if (text.length >= MIN_TEXT_LENGTH) text else "")
            }
        }
    }

    // ── Chiamata Gemini (REST, self-contained) ────────────────────────────────

    private fun callGemini(apiKey: String, model: String, prompt: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val body = JSONObject()
            .put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            ))
            .toString()

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Content-Type", "application/json")
        }
        conn.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val resp = stream.bufferedReader(StandardCharsets.UTF_8).readText()
        require(code in 200..299) { "HTTP $code: ${resp.take(600)}" }

        return JSONObject(resp)
            .getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
            .getString("text")
    }

    private data class Screen(val title: String, val body: String, val pages: String)

    private fun parseScreens(raw: String): List<Screen> {
        val stripped = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = stripped.indexOf('['); val end = stripped.lastIndexOf(']')
        val jsonStr = if (start >= 0 && end > start) stripped.substring(start, end + 1) else stripped
        val arr = JSONArray(jsonStr)
        return (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            val title = obj.optString("title").trim()
            val body = obj.optString("body").trim()
            if (title.isBlank() || body.isBlank()) return@mapNotNull null
            val pagesArr = obj.optJSONArray("sourcePageNums")
            val pages = (0 until (pagesArr?.length() ?: 0))
                .map { j -> pagesArr!!.optInt(j, 0) }.filter { it > 0 }.joinToString(", ")
            Screen(title, body, pages)
        }
    }

    // ── Report HTML ───────────────────────────────────────────────────────────

    private fun buildHtml(
        file: File, totalChunks: Int, cleanChunks: Int, context: String, prompt: String,
        coverage: List<TypeCoverage>, truncated: Boolean, model: String, llmCalled: Boolean,
        rawResponse: String?, screens: List<Screen>, llmError: String?
    ): String {
        val limit = LearningScreensPrompt.CONTEXT_CHAR_LIMIT
        val outTotal = coverage.sumOf { it.outCount }
        val sb = StringBuilder()
        sb.append(
            """
            <!DOCTYPE html><html lang="it"><head><meta charset="utf-8">
            <title>Learning Screens — ${esc(file.name)}</title>
            <style>
              :root { color-scheme: light dark; }
              body { font-family: -apple-system, Segoe UI, Roboto, sans-serif; margin: 24px; line-height: 1.45; }
              h1 { font-size: 20px; margin: 0 0 4px; } h2 { font-size: 16px; margin: 26px 0 8px;
                border-bottom: 2px solid #8884; padding-bottom: 4px; }
              .sub { color: #888; font-size: 13px; margin-bottom: 16px; }
              .cards { display: flex; flex-wrap: wrap; gap: 10px; margin: 12px 0; }
              .card { border: 1px solid #8884; border-radius: 10px; padding: 10px 14px; min-width: 130px; }
              .card .n { font-size: 22px; font-weight: 700; } .card .l { font-size: 12px; color: #888; }
              .warn { color: #e65100; font-weight: 700; }
              table { border-collapse: collapse; width: 100%; font-size: 13px; }
              th, td { text-align: left; padding: 6px 8px; border-bottom: 1px solid #8883; vertical-align: top; }
              td.num { text-align: right; font-variant-numeric: tabular-nums; }
              tr.cut { background: #ff6b6b22; }
              .badge { display: inline-block; padding: 2px 8px; border-radius: 999px; font-size: 11px;
                font-weight: 600; color: #fff; }
              pre { white-space: pre-wrap; background: #7771; padding: 12px; border-radius: 8px;
                font-size: 12px; max-height: 460px; overflow: auto; }
              .screen { border: 1px solid #8884; border-radius: 10px; padding: 12px 16px; margin: 10px 0; }
              .screen h3 { margin: 0 0 6px; font-size: 15px; }
              .screen .src { color: #888; font-size: 12px; margin-top: 6px; }
              details { margin: 8px 0; } summary { cursor: pointer; font-weight: 600; }
            </style></head><body>
            <h1>Learning Screens Inspector — ${esc(file.name)}</h1>
            <div class="sub">contesto flat troncato a $limit char · modello: ${esc(model)} ·
              ${if (llmCalled) "LLM chiamato" else "solo input (nessuna API key)"}</div>
            <div class="cards">
              ${card(totalChunks.toString(), "chunk totali")}
              ${card(cleanChunks.toString(), "chunk puliti")}
              ${card("${context.length}/$limit", if (truncated) "context (TRONCATO)" else "context char")}
              ${card(outTotal.toString(), "chunk fuori contesto")}
              ${card(if (llmCalled) screens.size.toString() else "—", "schermate generate")}
            </div>
            """.trimIndent()
        )

        if (truncated) {
            sb.append("<p class=\"warn\">⚠ Il contesto è troncato: $outTotal chunk puliti NON arrivano " +
                "all'LLM. Le sezioni in coda al regolamento (es. punteggio/fine partita) rischiano di " +
                "essere escluse — è il limite che la Fase 2 deve superare con la selezione per sezione.</p>")
        }

        sb.append("<h2>Copertura contesto per sezione</h2>")
        sb.append("<table><thead><tr><th>sectionType</th><th class=\"num\">chunk puliti</th>" +
            "<th class=\"num\">nel contesto</th><th class=\"num\">esclusi</th></tr></thead><tbody>")
        for (cov in coverage) {
            val rowClass = if (cov.outCount > 0) " class=\"cut\"" else ""
            sb.append("<tr$rowClass><td>${badge(cov.type)}</td><td class=\"num\">${cov.total}</td>" +
                "<td class=\"num\">${cov.inCount}</td><td class=\"num\">${cov.outCount}</td></tr>")
        }
        sb.append("</tbody></table>")

        if (llmCalled) {
            sb.append("<h2>Schermate generate (${screens.size})</h2>")
            if (llmError != null) {
                sb.append("<p class=\"warn\">Errore LLM: ${esc(llmError)}</p>")
            }
            for (s in screens) {
                sb.append("<div class=\"screen\"><h3>${esc(s.title)}</h3><div>${esc(s.body)}</div>" +
                    (if (s.pages.isNotBlank()) "<div class=\"src\">Fonti: p. ${esc(s.pages)}</div>" else "") +
                    "</div>")
            }
            if (rawResponse != null) {
                sb.append("<details><summary>Risposta grezza LLM</summary><pre>${esc(rawResponse)}</pre></details>")
            }
        }

        sb.append("<h2>Prompt inviato</h2><details open><summary>${prompt.length} caratteri</summary>" +
            "<pre>${esc(prompt)}</pre></details>")
        sb.append("</body></html>")
        return sb.toString()
    }

    private fun card(n: String, label: String) =
        "<div class=\"card\"><div class=\"n\">${esc(n)}</div><div class=\"l\">${esc(label)}</div></div>"

    private fun badge(type: String): String {
        val color = when (type) {
            "SETUP" -> "#2e7d32"; "GAMEPLAY" -> "#1565c0"; "PLAYER_TURN" -> "#6a1b9a"
            "ACTIONS" -> "#ad1457"; "END_ROUND" -> "#00838f"; "SCORING" -> "#e65100"
            "SPECIAL_RULES" -> "#4e342e"; else -> "#607d8b"
        }
        return "<span class=\"badge\" style=\"background:$color\">${esc(type)}</span>"
    }

    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun String?.ifBlankOrNull(default: String) = if (isNullOrBlank()) default else this

    companion object {
        private const val MIN_TEXT_LENGTH = 30
    }
}
