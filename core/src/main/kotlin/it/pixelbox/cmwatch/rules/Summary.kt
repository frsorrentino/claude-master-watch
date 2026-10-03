package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import java.time.ZoneId

/**
 * Il riepilogo unico (design 03/10, approvato da Franz): Sessioni e casa della master fuse. Ogni sessione una volta,
 * nell'ordine del bisogno: ti aspetta, ha finito, al lavoro, ferme; le chiuse a parte; le righe di servizio in fondo.
 * La master è la cornice (in basso, vicino al campo) e compare nella lista solo quando aspetta.
 */
object Summary {
    enum class Group { WAITING, FINISHED, WORKING, STILL }
    data class Row(val group: Group, val session: Session, val text: String?, val at: Long?, val key: String? = null)
    data class Model(val rows: List<Row>, val closed: List<Session>, val service: List<MasterHome.Row>, val open: Int, val master: Session?)

    private val SERVICE = setOf(MasterHome.Kind.CONTEXT, MasterHome.Kind.NIGHT_REPORT, MasterHome.Kind.NIGHT, MasterHome.Kind.NEXT_STEP, MasterHome.Kind.SCHEDULED)

    fun build(state: State, events: List<Event>, sent: List<Sent>, now: Long, zone: ZoneId, read: Set<String>): Model {
        val live = state.sessions.filter { it.state != SessionState.GONE }
        val master = live.firstOrNull { it.name == ContextActions.MASTER }
        val others = live.filter { it.name != ContextActions.MASTER }
        val forYou = MasterHome.forYou(state, events, sent, now, zone, read, limit = Int.MAX_VALUE).rows
        val waiting = live.filter { it.question != null }.sortedBy { it.question!!.askedAt }
            .map { Row(Group.WAITING, it, it.question!!.text, it.question!!.askedAt) }
        val finished = forYou.filter { it.kind == MasterHome.Kind.FINISHED }
            .mapNotNull { r -> others.firstOrNull { it.name == r.session }?.let { Row(Group.FINISHED, it, r.detail, r.at, r.key) } }
        val working = MasterHome.working(state).map { Row(Group.WORKING, it.session, it.detail, it.session.turnStarted ?: it.session.since) }
        val taken = (waiting + finished + working).map { it.session.name }.toSet()
        val still = others.filter { it.state == SessionState.IDLE && it.name !in taken }.sortedByDescending { it.since }
            .map { Row(Group.STILL, it, it.outcome?.full, it.since) }
        return Model(
            rows = waiting + finished + working + still,
            closed = state.sessions.filter { it.state == SessionState.GONE },
            service = forYou.filter { it.kind in SERVICE },
            open = others.size,
            master = master,
        )
    }
}
