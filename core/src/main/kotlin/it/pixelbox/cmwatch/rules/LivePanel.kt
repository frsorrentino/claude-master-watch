package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * La pillola della live aperta (variante B2, scelta da Franz l'08/10 alle 21:47): una riga per sessione, chi, che cosa fa
 * o che cosa ha fatto, e da quanto o a che ora. Prima chi chiede, poi chi lavora, poi le ferme; senza le chiuse.
 */
object LivePanel {
    enum class Kind { ASKING, WORKING, STILL }
    data class Row(val name: String, val kind: Kind, val what: String?, val minutes: Int?, val at: Long?)

    fun rows(state: State, now: Long): List<Row> = state.sessions.filter { it.state != SessionState.GONE }.map { s ->
        when {
            s.question != null -> Row(s.name, Kind.ASKING, s.question.text, null, s.question.askedAt)
            s.state == SessionState.BUSY || s.state == SessionState.AWAITING ->
                Row(s.name, Kind.WORKING, s.toolNote?.takeIf { it.isNotBlank() }, ((now - (s.turnStarted ?: s.since)) / 60).coerceAtLeast(0).toInt(), null)
            else -> Row(s.name, Kind.STILL, s.outcome?.short, null, s.outcome?.at ?: s.since)
        }
    }.sortedBy { it.kind.ordinal }
}
