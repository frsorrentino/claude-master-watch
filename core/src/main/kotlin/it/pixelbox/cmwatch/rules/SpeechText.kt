package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Outcome
import it.pixelbox.cmwatch.contract.Question
import it.pixelbox.cmwatch.contract.Recap

/**
 * Testi da leggere a voce (Franz, 14/09 12:17, «applica tutte»): la risposta intera chiesta al PC, la domanda con le
 * opzioni, il recap. Il markdown letto a voce diventa rumore («asterisco», «barra verticale») e si toglie; il codice si
 * annuncia soltanto. Il motore vocale di Android accetta un testo alla volta fino a
 * `TextToSpeech.getMaxSpeechInputLength()`, 4000 caratteri: un testo lungo si legge a pezzi tagliati a fine frase.
 */
object SpeechText {
    /** Pezzo massimo per una lettura: sotto il limite di Android, con margine. */
    const val MAX_CHUNK = 3500

    private const val FINE = ".!?:;,…"

    fun clean(text: String, codeLabel: String): String {
        var t = text.replace(Regex("```[\\s\\S]*?(```|$)"), "\n${Regex.escapeReplacement(codeLabel)}.\n")
        t = t.replace(Regex("`([^`]*)`"), "$1")
        t = t.replace(Regex("!?\\[([^\\]]*)]\\([^)]*\\)"), "$1")
        t = t.replace(Regex("https?://\\S+"), "")
        t = t.lines().joinToString("\n") { raw ->
            val line = raw.trim()
                .replace(Regex("^#{1,6}\\s*"), "")                  // titoli
                .replace(Regex("^[-*+]\\s+"), "")                   // elenchi puntati
                .replace(Regex("^\\|?\\s*:?-{3,}.*$"), "")          // separatori di tabella e righe orizzontali
                .replace(Regex("\\s*\\|\\s*"), ", ")                // celle di tabella
                .trim(' ', ',')
                .replace(Regex("(\\*\\*|\\*)(?=\\S)(.+?)(?<=\\S)\\1"), "$2")   // grassetto e corsivo
            // Una riga senza punteggiatura finale (titolo, voce di elenco) chiude con un punto: la voce fa la pausa.
            if (line.isEmpty() || line.last() in FINE) line else "$line."
        }
        return t.replace(Regex("[ \\t]+"), " ").replace(Regex("\\n{2,}"), "\n").trim()
    }

    /** Pezzi da leggere di fila, ognuno entro `max`, tagliati a fine frase; una frase più lunga si taglia a uno spazio. */
    fun chunks(text: String, max: Int = MAX_CHUNK): List<String> {
        val frasi = text.trim().split(Regex("(?<=[.!?…:;])\\s+|\\n+")).map { it.trim() }.filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        fun flush() { if (cur.isNotEmpty()) { out += cur.toString(); cur.setLength(0) } }
        for (f in frasi) {
            var rest = f
            while (rest.length > max) {
                flush()
                val cut = rest.lastIndexOf(' ', max).takeIf { it > 0 } ?: max
                out += rest.substring(0, cut).trim()
                rest = rest.substring(cut).trim()
            }
            if (cur.isNotEmpty() && cur.length + 1 + rest.length > max) flush()
            if (cur.isNotEmpty()) cur.append(' ')
            cur.append(rest)
        }
        flush()
        return out
    }

    /** La domanda con le opzioni numerate: «Deploy now? 1, yes. 2, no.» (`option` = «%1$d, %2$s.»). */
    fun question(q: Question, option: String): String =
        (listOf(q.text.trim()) + q.options.map { option.format(it.n, it.label.trim()) }).joinToString(" ")

    /** Il recap del giorno: per ogni progetto cosa ha fatto e il prossimo passo (`next` = «Prossimo: %1$s.»). */
    fun recap(recap: Recap, next: String): String = recap.items.joinToString(" ") { item ->
        val fatto = "${item.project}: ${item.done.trim().trimEnd('.')}."
        item.next?.trim()?.takeIf { it.isNotEmpty() }?.let { "$fatto ${next.format(it.trimEnd('.'))}" } ?: fatto
    }

    /**
     * L'Esito come si vede: il titolo, poi il testo sotto. Il ▶ legge quello che ha accanto, non la risposta intera,
     * che è di «Ascolta la risposta» sulla Scheda (Franz, 14/09 15:01: «tutti e 3 i punti leggono lo stesso testo»).
     */
    fun outcome(o: Outcome): String = listOfNotNull(OutcomeText.headline(o), OutcomeText.body(o)).joinToString("\n")

    /** Dopo aver chiesto il testo intero al PC: la sua risposta se è arrivata e dice qualcosa, altrimenti il ripiego. */
    fun pick(result: CmdResult?, fallback: String?): String? =
        result?.takeIf { it.ok && it.text.isNotBlank() }?.text ?: fallback?.takeIf { it.isNotBlank() }
}
