package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Terminale dal vivo (design del 15/09, dal confronto con Agent Watch): il relay ripubblica lo stato a ogni strumento e
 * l'orologio lo riceve già in streaming. Con il Terminale aperto, un cambio della sessione vale una nuova cattura; la
 * fine del turno (smette di lavorare) vale anche la risposta intera. Il contratto non cambia: il segnale c'era già.
 */
object TerminalLive {
    enum class Ask { SCREEN, SCREEN_AND_LAST }

    fun next(prev: Session?, cur: Session?): Ask? {
        if (prev == null || cur == null) return null
        if (prev.state == SessionState.BUSY && cur.state != SessionState.BUSY) return Ask.SCREEN_AND_LAST
        val moved = prev.state != cur.state || prev.since != cur.since || prev.tool != cur.tool ||
            prev.toolNote != cur.toolNote || prev.turnStarted != cur.turnStarted
        return if (moved) Ask.SCREEN else null
    }
}
