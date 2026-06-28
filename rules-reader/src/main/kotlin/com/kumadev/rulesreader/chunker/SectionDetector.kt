package com.kumadev.rulesreader.chunker

import com.kumadev.rulesreader.model.ExtractedPage
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tipi di sezione logica riconosciuti in un regolamento di gioco da tavolo.
 * Vengono salvati come [String] su [com.kumadev.rulesreader.db.entity.RulebookChunkEntity.sectionType]
 * per abilitare retrieval consapevole della struttura (Fase 3).
 */
object SectionType {
    const val SETUP = "SETUP"
    const val GAMEPLAY = "GAMEPLAY"
    const val PLAYER_TURN = "PLAYER_TURN"
    const val ACTIONS = "ACTIONS"
    const val END_ROUND = "END_ROUND"
    const val SCORING = "SCORING"
    const val SPECIAL_RULES = "SPECIAL_RULES"
    const val UNKNOWN = "UNKNOWN"

    val ALL: Set<String> = setOf(
        SETUP, GAMEPLAY, PLAYER_TURN, ACTIONS, END_ROUND, SCORING, SPECIAL_RULES, UNKNOWN
    )
}

/** Riga di testo con la pagina di origine, unità base per la ricostruzione dei chunk. */
data class SectionLine(val text: String, val pageNum: Int)

/**
 * Sezione logica rilevata: un blocco di righe consecutive che condividono lo stesso
 * [sectionType]. La prima riga è tipicamente il titolo di sezione.
 */
data class DetectedSection(
    val sectionType: String,
    val lines: List<SectionLine>
) {
    /** Pagina della prima riga della sezione (usata per la citazione del chunk). */
    val pageNum: Int get() = lines.firstOrNull()?.pageNum ?: 1

    /** Testo della sezione con le righe normalizzate e unite da spazio singolo. */
    val text: String
        get() = lines.joinToString(" ") { it.text.trim() }.trim()

    val wordCount: Int
        get() = text.split(WHITESPACE).count { it.isNotEmpty() }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}

/**
 * Riconosce i confini logici di un regolamento partendo dal testo estratto pagina per pagina.
 *
 * Strategia: analisi riga per riga. Una riga è considerata **heading** (nuovo confine di
 * sezione) se è breve e soddisfa almeno uno dei criteri:
 *  - titolo numerato ("1. Setup", "2.1 Azioni", "3) Turno")
 *  - riga quasi interamente in MAIUSCOLO
 *  - inizia con una keyword di dominio nota (IT/EN)
 *
 * Il tipo della sezione che segue viene derivato dal testo dell'heading tramite match di
 * keyword ordinate per specificità. Il testo prima del primo heading confluisce in una
 * sezione [SectionType.UNKNOWN] (intro/componenti generici).
 *
 * Se nel documento non si riconosce alcun heading (es. testo OCR privo di struttura),
 * viene restituita un'unica sezione [SectionType.UNKNOWN] con tutto il contenuto: in questo
 * modo il [RulesChunker] ricade automaticamente sullo sliding window puro.
 */
@Singleton
class SectionDetector @Inject constructor() {

    /** Numero massimo di parole per considerare una riga un potenziale titolo. */
    private val maxHeadingWords = 8

    /** Rapporto minimo di lettere maiuscole per classificare una riga come heading MAIUSCOLO. */
    private val upperCaseRatioThreshold = 0.70f

    /**
     * Keyword ordinate per specificità decrescente: la prima corrispondenza vince.
     * L'ordine è cruciale — es. "end of round" (END_ROUND) deve precedere "round" (GAMEPLAY),
     * altrimenti verrebbe assorbito dalla categoria più generica.
     */
    private val keywordGroups: List<Pair<String, List<String>>> = listOf(
        SectionType.END_ROUND to listOf(
            "end of round", "end of the round", "round end", "end of turn",
            "end phase", "cleanup", "clean up", "clean-up", "upkeep", "refresh",
            "fine del round", "fine round", "fine del turno", "fine turno", "riordino"
        ),
        SectionType.SCORING to listOf(
            "scoring", "final scoring", "score", "victory point", "victory points",
            "winning", "winner", "how to win", "end of game", "end of the game",
            "game end", "determine the winner",
            "punteggio", "punti vittoria", "vincitore", "chi vince", "come si vince",
            "fine partita", "fine della partita", "fine del gioco"
        ),
        SectionType.SETUP to listOf(
            "setup", "set up", "set-up", "preparation", "prepare", "getting started",
            "before you play", "before playing", "game setup", "components", "component",
            "contents", "materials",
            "preparazione", "prepara", "componenti", "contenuto", "materiali", "allestimento"
        ),
        SectionType.PLAYER_TURN to listOf(
            "your turn", "player turn", "on your turn", "turn order", "taking a turn",
            "a turn", "turno del giocatore", "il tuo turno", "turno di gioco", "turno"
        ),
        SectionType.ACTIONS to listOf(
            "actions", "action", "available actions", "main action",
            "azioni", "azione", "azioni disponibili"
        ),
        SectionType.GAMEPLAY to listOf(
            "gameplay", "game play", "how to play", "playing the game", "course of play",
            "sequence of play", "game round", "round structure", "rounds", "round",
            "phases", "phase", "overview", "object of the game", "objective",
            "aim of the game", "goal of the game",
            "svolgimento", "il gioco", "struttura del round", "fasi", "fase",
            "scopo del gioco", "obiettivo", "panoramica"
        ),
        SectionType.SPECIAL_RULES to listOf(
            "special", "variant", "variants", "advanced", "clarification", "clarifications",
            "faq", "appendix", "glossary", "solo", "solitaire", "two-player", "2-player",
            "2 player", "icons", "reference",
            "regole speciali", "varianti", "avanzato", "chiarimenti", "glossario",
            "riferimento", "esempio"
        )
    )

    /**
     * @param pages pagine estratte, ordinate per pageNum. Le pagine vuote vengono ignorate.
     * @return sezioni logiche consecutive. Vuota solo se non c'è testo.
     */
    fun detect(pages: List<ExtractedPage>): List<DetectedSection> {
        val lines: List<SectionLine> = pages
            .filter { it.rawText.isNotBlank() }
            .flatMap { page ->
                page.rawText.split('\n')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { SectionLine(it, page.pageNum) }
            }

        if (lines.isEmpty()) return emptyList()

        val sections = mutableListOf<DetectedSection>()
        var currentType = SectionType.UNKNOWN
        var currentLines = mutableListOf<SectionLine>()

        for (line in lines) {
            if (isHeading(line.text)) {
                if (currentLines.isNotEmpty()) {
                    sections.add(DetectedSection(currentType, currentLines))
                }
                currentType = classify(line.text)
                currentLines = mutableListOf(line)
            } else {
                currentLines.add(line)
            }
        }
        if (currentLines.isNotEmpty()) {
            sections.add(DetectedSection(currentType, currentLines))
        }

        return sections
    }

    // ── Rilevamento heading ──────────────────────────────────────────────────

    /** True se la riga ha l'aspetto di un titolo di sezione. */
    internal fun isHeading(rawLine: String): Boolean {
        val line = rawLine.trim()
        if (line.length < 2) return false

        val words = line.split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty() || words.size > maxHeadingWords) return false

        // Deve contenere almeno una lettera (scarta righe di soli numeri/simboli/pagine)
        if (line.none { it.isLetter() }) return false

        return isNumberedHeading(line) || isUpperCaseHeading(line) || isKeywordHeading(line)
    }

    /** "1. Setup", "2.1 Azioni", "3) Il tuo turno" — numero iniziale seguito da testo. */
    private fun isNumberedHeading(line: String): Boolean =
        NUMBERED_HEADING.containsMatchIn(line)

    /** Riga con almeno il 70% di lettere maiuscole (es. "SETUP", "COME SI GIOCA"). */
    private fun isUpperCaseHeading(line: String): Boolean {
        val letters = line.filter { it.isLetter() }
        // Almeno 3 lettere: esclude punti cardinali e sigle brevi (NE, SW, "W E", "N S").
        if (letters.length < 3) return false
        val upper = letters.count { it.isUpperCase() }
        return upper.toFloat() / letters.length >= upperCaseRatioThreshold
    }

    /** Riga breve che inizia con (o è) una keyword di dominio nota. */
    private fun isKeywordHeading(line: String): Boolean {
        if (line.split(WHITESPACE).size > 6) return false
        val cleaned = normalizeHeading(line)
        return keywordGroups.any { (_, keywords) ->
            keywords.any { kw -> cleaned == kw || cleaned.startsWith("$kw ") }
        }
    }

    // ── Classificazione tipo sezione ─────────────────────────────────────────

    /** Deriva il [SectionType] dal testo dell'heading; [SectionType.UNKNOWN] se nessun match. */
    internal fun classify(rawHeading: String): String {
        val cleaned = normalizeHeading(rawHeading)
        if (cleaned.isEmpty()) return SectionType.UNKNOWN
        for ((type, keywords) in keywordGroups) {
            if (keywords.any { kw -> containsKeyword(cleaned, kw) }) return type
        }
        return SectionType.UNKNOWN
    }

    /**
     * Match di keyword a **parola intera** (confine sia iniziale che finale), per evitare
     * falsi positivi da sottostringa (es. "solo" dentro "solomon", "score" dentro "scoreboard").
     */
    private fun containsKeyword(haystack: String, keyword: String): Boolean {
        var from = 0
        while (from <= haystack.length) {
            val idx = haystack.indexOf(keyword, from)
            if (idx < 0) return false
            val before = idx - 1
            val after = idx + keyword.length
            val boundaryBefore = before < 0 || !haystack[before].isLetterOrDigit()
            val boundaryAfter = after >= haystack.length || !haystack[after].isLetterOrDigit()
            if (boundaryBefore && boundaryAfter) return true
            from = idx + 1
        }
        return false
    }

    /** Rimuove numerazione iniziale e punteggiatura, comprime spazi, lowercase. */
    private fun normalizeHeading(line: String): String =
        line.lowercase()
            .replace(LEADING_NUMBERING, "")
            .replace(NON_ALPHANUM_EDGE, " ")
            .replace(WHITESPACE, " ")
            .trim()

    private companion object {
        val WHITESPACE = Regex("\\s+")
        // Numero (con eventuali sottolivelli) seguito da separatore e da una lettera.
        val NUMBERED_HEADING = Regex("^\\s*\\d+([.)]\\d+)*[.)]?\\s+\\p{L}")
        // Numerazione iniziale tipo "1. ", "2.1) ", "3 - " da rimuovere prima della classificazione.
        val LEADING_NUMBERING = Regex("^\\s*\\d+([.)]\\d+)*[.):\\-]?\\s+")
        // Punteggiatura ai bordi che disturba il match delle keyword.
        val NON_ALPHANUM_EDGE = Regex("[^\\p{L}\\p{N} ]+")
    }
}
