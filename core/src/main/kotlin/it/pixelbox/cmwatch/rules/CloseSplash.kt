package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Il pannello di chiusura di una sessione (Franz, 08/10 18:06-18:29, variante A): la pagina non sparisce più. Dopo /exit
 * un pannello sopra la pagina dice che si sta chiudendo, con i passi veri (consegnato, confermato dal PC); poi che è chiusa
 * e a che ora, con «Riapri» e «Torna alla home», e dopo [CLOSED_SHOW_MS] si torna alla home. Gli stessi valori nella web.
 */
object CloseSplash {
    /** Quanto resta il pannello «è chiusa» prima del ritorno alla home. */
    const val CLOSED_SHOW_MS = 3_000L
    /** Oltre questi secondi senza conferma il pannello dice che ci sta mettendo più del solito. */
    const val SLOW_S = 20L

    /** La chiusura chiesta da qui: quale sessione, quando, con quale comando. */
    data class Leaving(val name: String, val sentAt: Long, val cmd: String?)

    sealed interface Phase {
        data class Closing(val name: String, val sentAt: Long, val delivered: Boolean, val slow: Boolean) : Phase
        data class Closed(val name: String, val at: Long, val byMe: Boolean) : Phase
    }

    /**
     * La fase per la pagina di [name]; null = niente pannello. [lastSeen] è la sessione com'era l'ultima volta nello stato,
     * per l'ora di chiusura quando non c'è più; [delivered] dice se il PC ha risposto al comando di chiusura.
     * Chiusa per conto suo ma ancora nello stato resta una pagina con «Riapri» (03/10 19:57): il pannello arriva solo
     * quando esce dallo stato.
     */
    fun of(name: String, sessions: List<Session>, leaving: Leaving?, lastSeen: Session?, now: Long, delivered: Boolean = false): Phase? {
        val s = sessions.firstOrNull { it.name == name }
        val mine = leaving?.name == name
        // Per una gone `since` è l'ultimo avvistamento, cioè quando si è chiusa; per una viva no: allora vale adesso.
        val at = s?.since ?: lastSeen?.takeIf { it.state == SessionState.GONE }?.since ?: now
        return when {
            s == null -> Phase.Closed(name, at, byMe = mine)
            s.state == SessionState.GONE -> if (mine) Phase.Closed(name, at, byMe = true) else null
            // Dal vivo 08/10 19:29: dopo /exit Claude Code può chiedere cosa fare dei lavori in background; il pannello non
            // copre la domanda, e torna quando è risposta.
            mine && s.question != null -> null
            mine -> Phase.Closing(name, leaving!!.sentAt, delivered, slow = now - leaving.sentAt >= SLOW_S)
            else -> null
        }
    }
}
