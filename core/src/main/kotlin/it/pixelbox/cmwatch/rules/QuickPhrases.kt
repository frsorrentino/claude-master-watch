package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State

/**
 * Le frasi rapide sopra la barra (piano 30/09, Task 6): i messaggi mandati più spesso nel progetto della sessione (da
 * qualunque sua sessione), almeno due volte, al massimo tre, mai uguali al prompt suggerito che sta già lì sopra.
 * Maiuscole e spazi non contano per contare; si mostra la forma usata più di recente.
 */
object QuickPhrases {
    const val MAX = 3
    const val MIN_USES = 2

    fun of(sent: List<Sent>, state: State, session: Session, suggestion: String?): List<String> {
        val sameProject = state.sessions.filter { it.project == session.project }.map { it.name }.toSet() + session.name
        val skip = suggestion?.let(::norm)
        return sent.filter { it.session in sameProject && it.text.isNotBlank() && it.attachment == null }
            .groupBy { norm(it.text) }
            .filterKeys { it != skip }
            .filterValues { it.size >= MIN_USES }
            .entries.sortedWith(compareByDescending<Map.Entry<String, List<Sent>>> { it.value.size }.thenByDescending { e -> e.value.maxOf { it.sentAt } })
            .take(MAX)
            .map { e -> e.value.maxBy { it.sentAt }.text.trim() }
    }

    private fun norm(s: String) = s.trim().replace(Regex("\\s+"), " ").lowercase()
}
