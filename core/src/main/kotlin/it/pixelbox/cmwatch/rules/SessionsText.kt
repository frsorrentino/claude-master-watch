package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/** Testi puri della lista Sessioni: icona e pallino dell'account sono composable, qui solo «nome · durata». */
object SessionsText {
    fun row(s: Session, now: Long): String {
        val from = when (s.state) {
            SessionState.WAITING -> s.question?.askedAt ?: s.since
            SessionState.BUSY -> s.turnStarted ?: s.since
            SessionState.GONE -> return s.name
            else -> s.since
        }
        return "${s.name} · ${Durations.since(from, now)}"
    }

    /**
     * Sottotitolo della riga nella lista. Per una sessione finita dice a parole che è chiusa e da quando non si
     * vede: il badge grigio con la ✗ da solo non diceva se fosse aperta o chiusa (Franz, 13/09 18:01).
     */
    fun sub(s: Session, now: Long, closed: String): String? = when (s.state) {
        SessionState.GONE -> "$closed · ${Durations.since(s.since, now)}"
        SessionState.WAITING -> Durations.since(s.question?.askedAt ?: s.since, now)
        SessionState.BUSY, SessionState.AWAITING -> Durations.since(s.turnStarted ?: s.since, now)
        SessionState.IDLE -> Durations.since(s.since, now)
    }

    fun header(list: List<Session>, sessionsLabel: String): String {
        val q = list.count { it.question != null }
        val g = list.count { it.state == SessionState.GONE }
        val parts = mutableListOf("${list.size} $sessionsLabel")
        if (q > 0) parts += "$q ❓"
        if (g > 0) parts += "$g ✗"
        return parts.joinToString(" · ")
    }
}
