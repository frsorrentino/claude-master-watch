package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/** Testi puri della Scheda: «nome · account · stato · durata [· tool]» e «→ prossimo». Le etichette di stato arrivano da strings.xml. */
object CardText {
    fun header(s: Session, now: Long, stateLabels: Map<SessionState, String>): String {
        val parts = mutableListOf(s.name, s.account, stateLabels[s.state] ?: s.state.name.lowercase())
        if (s.state != SessionState.GONE) {
            val from = when (s.state) {
                SessionState.WAITING -> s.question?.askedAt ?: s.since
                SessionState.BUSY -> s.turnStarted ?: s.since
                else -> s.since
            }
            parts += Durations.since(from, now)
        }
        s.tool?.takeIf { s.state == SessionState.BUSY }?.let { parts += it }
        return parts.joinToString(" · ")
    }

    fun next(s: Session): String? = s.next?.let { "→ $it" }
}
