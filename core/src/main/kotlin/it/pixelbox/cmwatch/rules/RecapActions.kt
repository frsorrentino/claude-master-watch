package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * Le Azioni della sezione Recap (mockup approvato da Franz l'08/10 alle 21:08): quelle delle sessioni vive e le «prossimo»
 * del recap del giorno, ognuna con a chi va. Un'azione di una sessione va a lei; una del recap va alla sessione viva del
 * progetto, se c'è, altrimenti alla master come «Riprendi progetto: …». Senza doppioni, al massimo [MAX].
 */
object RecapActions {
    const val MAX = 8

    /**
     * `text` = quello che si legge, `send` = quello che parte, `to` = la sessione che lo riceve, `from` = da dove viene;
     * `recap` = viene dal recap del giorno (l'etichetta del tasto dice «recap dd/mm», il foglio «Dal recap del …»);
     * `agenda` = un «Fallo» di una riga dell'agenda, che va alla master.
     */
    data class Action(
        val text: String, val send: String, val to: String, val from: String, val viaMaster: Boolean,
        val recap: Boolean = false, val agenda: Boolean = false,
    )

    fun of(state: State, resume: String = "Riprendi %1\$s: %2\$s"): List<Action> {
        val live = state.sessions.filter { it.state != SessionState.GONE && it.name != ContextActions.MASTER }
        val fromSessions = live.flatMap { s -> s.nextSteps.orEmpty().map { Action(it.text, it.text, s.name, s.name, false) } }
        val fromRecap = state.recap.items.mapNotNull { r ->
            val next = r.next?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val s = live.firstOrNull { it.name == r.project || it.project.substringAfterLast('/') == r.project }
            if (s != null) Action(next, next, s.name, r.project, false, recap = true)
            else Action(next, resume.format(r.project, next), ContextActions.MASTER, r.project, true, recap = true)
        }
        return (fromSessions + fromRecap).distinctBy { NextSteps.key(it.text) }.take(MAX)
    }
}
