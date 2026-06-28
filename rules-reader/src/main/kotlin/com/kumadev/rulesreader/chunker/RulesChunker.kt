package com.kumadev.rulesreader.chunker

import com.kumadev.rulesreader.model.ExtractedPage
import com.kumadev.rulesreader.model.RulesChunk
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Structure-Aware Chunker (Fase 1).
 *
 * Usa i confini logici di [SectionDetector] come punti di taglio **preferenziali**, ma con una
 * politica di accumulo che garantisce chunk di dimensione utile all'embedding anche su
 * regolamenti con estrazione PDF rumorosa (etichette mappa, testo grafico MAIUSCOLO, ecc.).
 *
 * Politica:
 * - Le sezioni vengono accumulate in un buffer; il buffer viene emesso come chunk **solo a un
 *   confine di sezione** e **solo** quando ha raggiunto [TARGET_WORDS]. Così i confini dei chunk
 *   si allineano ai titoli reali senza produrre micro-chunk da 1–2 parole quando il detector
 *   sovra-segmenta.
 * - A ogni chunk viene assegnato il `sectionType` **dominante** (il tipo che contribuisce più
 *   parole; i tipi noti prevalgono su UNKNOWN).
 * - Una singola sezione più grande di [MAX_SECTION_WORDS] viene sotto-suddivisa con sliding
 *   window ([CHUNK_WORDS]/[OVERLAP_WORDS]) — fallback interno per sezioni molto lunghe.
 *
 * Se il documento non ha struttura riconoscibile, [SectionDetector] restituisce un'unica sezione
 * [SectionType.UNKNOWN] e il comportamento degenera in sliding window puro.
 */
@Singleton
class RulesChunker @Inject constructor(
    private val sectionDetector: SectionDetector
) {

    companion object {
        const val CHUNK_WORDS = 500
        const val OVERLAP_WORDS = 100

        /** Dimensione target del buffer: raggiunta, si emette al confine di sezione successivo. */
        const val TARGET_WORDS = 350

        /** Oltre questa soglia una singola sezione viene sotto-suddivisa con sliding window. */
        const val MAX_SECTION_WORDS = 500

        /** Coda residua minima nello sliding window: sotto questa soglia si interrompe. */
        const val MIN_TAIL_WORDS = 20

        /**
         * Quota minima di parole che un tipo noto deve occupare in un chunk per etichettarlo.
         * Sotto questa soglia il chunk resta UNKNOWN: evita che una micro-intestazione
         * (es. un'intestazione di tabella "Action Result…") dirotti l'etichetta di un chunk
         * grande e per lo più non strutturato (es. l'introduzione narrativa).
         */
        const val SECTION_LABEL_MIN_SHARE = 0.20

        private val WHITESPACE = Regex("\\s+")
    }

    private data class WordEntry(val word: String, val pageNum: Int)

    /**
     * @param pages Lista ordinata per pageNum di pagine estratte.
     * @return Lista di [RulesChunk] con indice progressivo, numero di pagina e sectionType.
     */
    fun chunk(pages: List<ExtractedPage>): List<RulesChunk> {
        val sections = sectionDetector.detect(pages)
        if (sections.isEmpty()) return emptyList()

        val chunks = mutableListOf<RulesChunk>()
        var chunkIndex = 0

        val buffer = mutableListOf<WordEntry>()
        val typeWords = HashMap<String, Int>()

        fun flush() {
            if (buffer.isEmpty()) return
            chunks.add(buildChunk(chunkIndex++, buffer, dominantType(typeWords)))
            buffer.clear()
            typeWords.clear()
        }

        for (section in sections) {
            val words: List<WordEntry> = section.lines.flatMap { line ->
                line.text.split(WHITESPACE)
                    .filter { it.isNotEmpty() }
                    .map { WordEntry(it, line.pageNum) }
            }
            if (words.isEmpty()) continue

            if (words.size > MAX_SECTION_WORDS) {
                // Sezione troppo grande: chiude il buffer e la sotto-suddivide con sliding window.
                flush()
                slidingWindow(words) { slice ->
                    chunks.add(buildChunk(chunkIndex++, slice, section.sectionType))
                }
                continue
            }

            buffer.addAll(words)
            typeWords[section.sectionType] = (typeWords[section.sectionType] ?: 0) + words.size

            // Emette solo a confine di sezione (qui, tra una sezione e la successiva) quando pieno.
            if (buffer.size >= TARGET_WORDS) flush()
        }
        flush()

        return chunks
    }

    /** Sliding window con overlap interno a una sezione grande. */
    private inline fun slidingWindow(
        words: List<WordEntry>,
        emit: (List<WordEntry>) -> Unit
    ) {
        var start = 0
        while (start < words.size) {
            val end = minOf(start + CHUNK_WORDS, words.size)
            emit(words.subList(start, end))

            start += CHUNK_WORDS - OVERLAP_WORDS
            // La coda residua rientra già nell'overlap del chunk precedente: stop.
            if (start < words.size && (words.size - start) < MIN_TAIL_WORDS) break
        }
    }

    private fun buildChunk(index: Int, words: List<WordEntry>, sectionType: String): RulesChunk =
        RulesChunk(
            index = index,
            pageNum = words.first().pageNum,
            text = words.joinToString(" ") { it.word },
            sectionType = sectionType
        )

    /**
     * Tipo dominante di un buffer: il tipo noto (≠ UNKNOWN) con più parole, ma solo se occupa
     * almeno [SECTION_LABEL_MIN_SHARE] delle parole totali del chunk; altrimenti UNKNOWN.
     * In questo modo una sezione SETUP sostanziale circondata da etichette non classificate
     * resta SETUP, mentre una micro-intestazione dentro un chunk grande non lo dirotta.
     */
    private fun dominantType(typeWords: Map<String, Int>): String {
        if (typeWords.isEmpty()) return SectionType.UNKNOWN
        val total = typeWords.values.sum()
        if (total == 0) return SectionType.UNKNOWN
        val bestKnown = typeWords.entries
            .filter { it.key != SectionType.UNKNOWN }
            .maxByOrNull { it.value }
            ?: return SectionType.UNKNOWN
        return if (bestKnown.value.toDouble() / total >= SECTION_LABEL_MIN_SHARE) {
            bestKnown.key
        } else {
            SectionType.UNKNOWN
        }
    }
}
