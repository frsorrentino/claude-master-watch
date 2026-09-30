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

    /**
     * Il tasto della barra di scrittura (design 30/09, parti 1 e 7): come in Claude Code, Stop mentre la sessione lavora
     * e il campo è vuoto, e solo se il relay sa fermare (`ops` con «interrupt»); appena si scrive torna Invia. Invia è
     * tonale quando la prima opzione della domanda è il bottone pieno; una sessione chiusa ha solo Riapri.
     */
    enum class Composer { SEND, SEND_TONAL, STOP, REOPEN, NONE }

    fun composer(s: Session, draft: String, ops: List<String>?): Composer = when {
        s.state == SessionState.GONE -> Composer.REOPEN
        draft.isNotBlank() -> if (button(s, draft) == Button.OPTION) Composer.SEND_TONAL else Composer.SEND
        (s.state == SessionState.BUSY || s.state == SessionState.AWAITING) && ops?.contains("interrupt") == true -> Composer.STOP
        else -> Composer.NONE
    }
}
