package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Il terminale dal vivo del telefono (restyling 30/09), come sull'orologio: una lettura all'apertura, poi una ogni
 * `POLL_MS` dopo la risposta alla precedente. Mai due letture in volo, mai per una sessione chiusa o sparita.
 */
object PhoneTerminal {
    const val POLL_MS = 4_000L

    fun shouldAsk(session: Session?, lastAskedAt: Long?, answered: Boolean, now: Long): Boolean = when {
        session == null || session.state == SessionState.GONE -> false
        lastAskedAt == null -> true
        !answered -> false
        else -> now - lastAskedAt >= POLL_MS
    }
}
