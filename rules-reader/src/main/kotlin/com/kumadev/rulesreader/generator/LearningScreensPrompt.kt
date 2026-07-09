package com.kumadev.rulesreader.generator

/**
 * Logica **pura** (senza Android/DB/network) per la costruzione del contesto e del prompt
 * delle schermate di apprendimento.
 *
 * È condivisa tra [LearningScreensGenerator] (produzione) e il tool di ispezione JVM
 * (`LearningScreensInspectorTest`), così ciò che si testa su PC è ESATTAMENTE ciò che gira
 * sul device — nessuna divergenza.
 */
object LearningScreensPrompt {

    /** Soglia: chunk con più del 40% di token singola-lettera-minuscola sono rumorosi. */
    const val NOISE_THRESHOLD = 0.40f

    /** Limite caratteri del contesto LLM per stare nel budget di token. */
    const val CONTEXT_CHAR_LIMIT = 10_000

    val SCREEN_TOPICS = listOf(
        "Panoramica",
        "Obiettivo",
        "Preparazione",
        "Il tuo turno",
        "Fine partita",
        "Regole speciali"
    )

    /** Input minimale di un chunk, indipendente da Room. */
    data class ChunkInput(
        val pageNum: Int,
        val text: String,
        val sectionType: String? = null
    )

    /** Filtra i chunk rumorosi (legature OpenType frammentate ecc.). */
    fun selectCleanChunks(chunks: List<ChunkInput>): List<ChunkInput> =
        chunks.filter { noiseRatio(it.text) <= NOISE_THRESHOLD }

    /**
     * Costruisce il testo di contesto: dump flat dei chunk puliti con annotazione pagina,
     * troncato a [CONTEXT_CHAR_LIMIT] caratteri.
     *
     * NB: la troncatura flat è il limite noto da superare in Fase 2 (selezione per sezione).
     */
    fun buildContext(cleanChunks: List<ChunkInput>): String =
        cleanChunks
            .joinToString("\n\n") { "[p.${it.pageNum}]\n${it.text}" }
            .take(CONTEXT_CHAR_LIMIT)

    fun buildPrompt(contextText: String): String = buildString {
        appendLine("Sei un assistente specializzato in giochi da tavolo.")
        appendLine("Analizza il testo estratto dal regolamento e crea schermate di apprendimento in ITALIANO.")
        appendLine()
        appendLine("Genera un array JSON con le schermate. Ogni schermata deve coprire uno degli argomenti")
        appendLine("seguenti (includi solo quelli per cui hai abbastanza informazioni nel testo):")
        SCREEN_TOPICS.forEachIndexed { i, topic -> appendLine("${i + 1}. \"$topic\"") }
        appendLine()
        appendLine("Ogni oggetto dell'array deve avere ESATTAMENTE questi campi:")
        appendLine("  - \"title\": string — usa esattamente i nomi della lista sopra")
        appendLine("  - \"body\": string — spiegazione chiara e concisa in italiano (max 200 parole)")
        appendLine("  - \"sourcePageNums\": array di interi — numeri di pagina da cui provengono le info")
        appendLine()
        appendLine("IMPORTANTE: rispondi SOLO con il JSON array valido. Zero testo aggiuntivo.")
        appendLine()
        appendLine("TESTO DEL REGOLAMENTO:")
        appendLine("---")
        append(contextText)
        appendLine()
        append("---")
    }

    /**
     * Rapporto di rumore: proporzione di token che sono singole lettere minuscole.
     * Indicatore dei chunk con legature OpenType frammentate (es. "I l l u s t r a t i o n").
     */
    fun noiseRatio(text: String): Float {
        val words = text.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (words.isEmpty()) return 1f
        val singleLower = words.count { w -> w.length == 1 && w[0].isLetter() && w[0].isLowerCase() }
        return singleLower.toFloat() / words.size
    }
}
