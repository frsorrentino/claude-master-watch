package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Il terminale dal vivo del telefono (restyling 30/09), come sull'orologio: una lettura all'apertura, poi una ogni
 * `POLL_MS` dopo la risposta alla precedente. Mai due letture in volo, mai per una sessione chiusa o sparita.
 */
object PhoneTerminal {
    const val POLL_MS = 4_000L

    /**
     * Una lettura senza risposta dopo il timeout dei comandi è persa (revisione 30/09): il `Repo` la segna fallita e non
     * scrive mai il risultato. Da lì si chiede di nuovo, invece di restare fermi per sempre.
     */
    const val LOST_MS = it.pixelbox.cmwatch.transport.Transport.RESULT_TIMEOUT_MS + 1_000L

    fun shouldAsk(session: Session?, lastAskedAt: Long?, answered: Boolean, now: Long): Boolean = when {
        session == null || session.state == SessionState.GONE -> false
        lastAskedAt == null -> true
        !answered -> now - lastAskedAt >= LOST_MS
        else -> now - lastAskedAt >= POLL_MS
    }

    /**
     * La chat legge molto meno del terminale (dal vivo 30/09 23:00): per il relay una lettura costa 5-8 s, e ogni 4 s i
     * prompt restavano in coda oltre i 20 s. Ogni 10 s mentre la sessione lavora, ogni minuto quando è ferma; in più una
     * lettura quando lo stato della sessione cambia (`TerminalLive.next`), decisa da chi chiama.
     */
    const val CHAT_BUSY_MS = 10_000L
    const val CHAT_IDLE_MS = 60_000L

    fun shouldAskChat(session: Session?, lastAskedAt: Long?, answered: Boolean, now: Long): Boolean {
        if (session == null || session.state == SessionState.GONE) return false
        if (lastAskedAt == null) return true
        if (!answered) return now - lastAskedAt >= LOST_MS
        val busy = session.state == SessionState.BUSY || session.state == SessionState.AWAITING
        return now - lastAskedAt >= if (busy) CHAT_BUSY_MS else CHAT_IDLE_MS
    }
}
