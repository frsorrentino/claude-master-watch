package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/** Il solo bottone pieno della scheda sessione (design 29/09) e dove va il testo scritto. */
object PhonePrimary {
    enum class Button { SEND, REOPEN, NONE }
    enum class Target { ANSWER_TEXT, PROMPT }

    fun button(s: Session, draft: String): Button = when {
        s.state == SessionState.GONE -> Button.REOPEN
        draft.isNotBlank() -> Button.SEND
        else -> Button.NONE
    }

    /** Risposta libera se la domanda c'è ancora, altrimenti prompt; null se non si può mandare. */
    fun target(s: Session, draft: String): Target? = when {
        s.state == SessionState.GONE || draft.isBlank() -> null
        s.question != null -> Target.ANSWER_TEXT
        else -> Target.PROMPT
    }
}
