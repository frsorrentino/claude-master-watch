package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import java.text.Normalizer

/**
 * La ricerca del telefono (piano 30/09, Task 5): nei messaggi mandati, negli esiti dei loro turni e negli eventi del
 * diario. Senza accenti né maiuscole («perche» trova «Perché»); il risultato è la riga trovata con la parte da mettere in
 * grassetto, dalla più recente.
 */
object ChatSearch {
    enum class Kind { SENT, OUTCOME, EVENT, CONVERSATION }

    /**
     * `start`/`end`: la parte trovata dentro `line`, nel testo originale. `ref`: unico per risultato, per le liste.
     * `live`: solo per i risultati del relay, se la sessione è ancora aperta.
     */
    data class Hit(
        val session: String?, val at: Long, val line: String, val start: Int, val end: Int, val kind: Kind, val ref: String = "",
        val live: Boolean? = null,
    )

    /**
     * Contratto 1.27: i risultati del relay, cercati nelle conversazioni di tutte le sessioni. Contengono già i messaggi
     * mandati e gli esiti, quindi del telefono restano solo le voci del registro senza sessione. Dal più recente.
     */
    fun withConversations(local: List<Hit>, page: it.pixelbox.cmwatch.contract.SearchPage): List<Hit> {
        val remote = page.hits.mapIndexed { i, h ->
            val start = (h.match.getOrNull(0) ?: 0).coerceIn(0, h.snippet.length)
            val end = (h.match.getOrNull(1) ?: start).coerceIn(start, h.snippet.length)
            Hit(h.session, h.at ?: 0, h.snippet, start, end, Kind.CONVERSATION, "CONV-${h.session}-${h.entry ?: i}-$i", h.live)
        }
        return (remote + local.filter { it.kind == Kind.EVENT && it.session == null }).sortedByDescending { it.at }
    }

    fun find(query: String, sent: List<Sent>, events: List<Event>): List<Hit> {
        val q = fold(query.trim())
        if (q.isEmpty()) return emptyList()
        val hits = mutableListOf<Hit>()
        fun scan(text: String?, session: String?, at: Long, kind: Kind, ref: String) {
            // La prima riga che contiene il testo cercato: un risultato per testo, non uno per riga.
            text?.lines()?.firstNotNullOfOrNull { raw ->
                val line = raw.trim()
                fold(line).indexOf(q).takeIf { it >= 0 }?.let { i -> Hit(session, at, line, i, i + q.length, kind, "$kind-$ref") }
            }?.let(hits::add)
        }
        sent.forEach { m ->
            scan(m.text, m.session, m.sentAt, Kind.SENT, m.id)
            scan(m.outcomeFull, m.session, m.doneAt ?: m.sentAt, Kind.OUTCOME, m.id)
        }
        events.forEachIndexed { i, e -> scan(listOf(e.title, e.body).filter { it.isNotBlank() }.joinToString("\n"), e.session, e.ts, Kind.EVENT, "${e.key}-$i") }
        return hits.sortedByDescending { it.at }
    }

    /** Minuscole e senza segni diacritici, carattere per carattere: gli indici restano quelli del testo originale. */
    fun fold(s: String): String = buildString(s.length) {
        s.forEach { c ->
            val base = Normalizer.normalize(c.toString(), Normalizer.Form.NFD).firstOrNull() ?: c
            append(base.lowercaseChar())
        }
    }
}
