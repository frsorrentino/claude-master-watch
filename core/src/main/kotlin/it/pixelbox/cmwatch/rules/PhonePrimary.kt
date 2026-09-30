package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Il solo bottone pieno della scheda sessione (design 29/09) e dove va il testo scritto. Con una domanda che ha opzioni
 * il bottone pieno è la prima opzione, come sull'orologio, e «Invia» resta tonale (restyling 30/09).
 */
object PhonePrimary {
    enum class Button { OPTION, SEND, REOPEN, NONE }
    enum class Target { ANSWER_TEXT, PROMPT }

    fun button(s: Session, draft: String): Button = when {
        s.state == SessionState.GONE -> Button.REOPEN
        !s.question?.options.isNullOrEmpty() -> Button.OPTION
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
