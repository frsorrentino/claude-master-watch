package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/**
 * Il menu delle sessioni in alto, a pannello (Franz, 03/10 16:44, variante A): le sessioni vive nei gruppi del riepilogo
 * (ti aspetta, al lavoro, ferme), nell'ordine ricevuto dentro ogni gruppo; la master no, sta nella home in testa al menu;
 * le chiuse solo contate, per la riga «Chiuse · N».
 */
object SessionsMenu {
    data class Model(val groups: List<Pair<Summary.Group, List<Session>>>, val closed: Int)

    fun of(sessions: List<Session>): Model {
        val live = sessions.filter { it.state != SessionState.GONE && it.name != ContextActions.MASTER }
        val group: (Session) -> Summary.Group = { s ->
            when {
                s.question != null -> Summary.Group.WAITING
                s.state == SessionState.BUSY || s.state == SessionState.AWAITING -> Summary.Group.WORKING
                else -> Summary.Group.STILL
            }
        }
        val byGroup = live.groupBy(group)
        return Model(
            groups = listOf(Summary.Group.WAITING, Summary.Group.WORKING, Summary.Group.STILL).mapNotNull { g -> byGroup[g]?.let { g to it } },
            closed = sessions.count { it.state == SessionState.GONE },
        )
    }
}
