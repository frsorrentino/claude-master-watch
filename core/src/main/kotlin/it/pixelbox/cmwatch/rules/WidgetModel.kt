package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * Il widget della schermata home (piano 30/09, Task 7; spec «Widget»), nello stile di ads-widget: una card per riga con
 * l'arco a sinistra e tre colonne scelte alla posa. Tre modi: un account (arco = 5 ore), una sessione (arco = contesto,
 * tocco = la sua scheda), la regia (arco = la quota più piena, poi una card per sessione viva). Qui i valori; etichette,
 * icone e colori stanno nell'app. Senza stato una card vuota, che l'app dice «in attesa del PC».
 */
object WidgetModel {
    enum class Mode { ACCOUNT, SESSION, BOARD }
    enum class Metric { WEEK, WORKING, WAITING, IDLE, NIGHT, CONTEXT, TURN_AGE, OUTCOME }

    data class Config(val mode: Mode, val target: String?, val metrics: List<Metric>, val opacity: Int, val corners: Int, val mono: Boolean)

    /** `session`: la scheda che il tocco apre; null = la Panoramica. */
    data class Card(
        val title: String, val arcPct: Int?, val arcLabel: String, val columns: List<Pair<Metric, String>>, val updatedAt: Long?,
        val session: String? = null,
        /** L'anello interno come nella Panoramica: la settimana dell'account; null dove non c'è. */
        val innerPct: Int? = null,
        /** Lo stato di una sessione (colore della riga), null per le card di account e regia. */
        val seg: WorkPanel.Seg? = null,
    )

    /** Etichette dell'arco, uguali in ogni lingua come «5h» e «ctx» sul polso. */
    const val ARC_5H = "5h"
    const val ARC_CTX = "ctx"
    /** Un valore che non si legge. */
    const val NONE = "–"
    const val MAX_COLUMNS = 3

    /** Il titolo della testata con l'iniziale maiuscola (Franz, 01/10 15:51): «penguin» → «Penguin». */
    fun heading(title: String): String = title.replaceFirstChar { it.titlecase() }

    fun defaults(mode: Mode): List<Metric> = when (mode) {
        Mode.ACCOUNT -> listOf(Metric.WEEK, Metric.WORKING, Metric.WAITING)
        Mode.SESSION -> listOf(Metric.TURN_AGE, Metric.OUTCOME, Metric.WEEK)
        Mode.BOARD -> listOf(Metric.WAITING, Metric.WORKING, Metric.IDLE)
    }

    fun cards(state: State?, config: Config, now: Long): List<Card> {
        val metrics = config.metrics.distinct().take(MAX_COLUMNS)
        if (state == null) return listOf(Card("", null, "", metrics.map { it to NONE }, null))
        return when (config.mode) {
            Mode.ACCOUNT -> {
                val name = Accounts.resolve(state, config.target.orEmpty())
                val q = name?.let { state.quota[it] }
                val scope = state.sessions.filter { it.account.equals(name, ignoreCase = true) }
                val fresh = q?.takeUnless { it.stale }
                listOf(Card(name.orEmpty(), fresh?.h5, ARC_5H, metrics.map { it to value(it, state, scope, name, now) }, state.ts, innerPct = fresh?.w7))
            }
            Mode.SESSION -> {
                val s = pick(state, config.target) ?: return listOf(Card(config.target.orEmpty(), null, ARC_CTX, metrics.map { it to NONE }, state.ts))
                listOf(sessionCard(state, s, metrics, now))
            }
            Mode.BOARD -> {
                val fullest = state.quota.values.filter { !it.stale && it.h5 != null }.maxByOrNull { it.h5!! }
                val board = Card(state.host, fullest?.h5, ARC_5H, metrics.map { it to value(it, state, state.sessions, null, now) }, state.ts, innerPct = fullest?.w7)
                val live = PhoneBoard.sections(state).filter { it.group != PhoneBoard.Group.CLOSED }.flatMap { it.sessions }
                listOf(board) + live.map { sessionCard(state, it, defaults(Mode.SESSION), now) }
            }
        }
    }

    /** La sessione scelta se è ancora nello stato, altrimenti la seguita, altrimenti la prima viva nell'ordine della regia. */
    private fun pick(state: State, target: String?): Session? =
        state.sessions.firstOrNull { it.name == target }
            ?: state.sessions.firstOrNull { it.followed && it.state != SessionState.GONE }
            ?: PhoneBoard.sections(state).filter { it.group != PhoneBoard.Group.CLOSED }.flatMap { it.sessions }.firstOrNull()

    private fun sessionCard(state: State, s: Session, metrics: List<Metric>, now: Long) =
        Card(s.name, s.context, ARC_CTX, metrics.map { it to value(it, state, listOf(s), s.account, now) }, state.ts, session = s.name,
            seg = WorkPanel.now(state.copy(sessions = listOf(s))).segments.firstOrNull())

    private fun value(m: Metric, state: State, scope: List<Session>, account: String?, now: Long): String {
        val counts = WorkPanel.now(state.copy(sessions = scope))
        return when (m) {
            Metric.WEEK -> (account?.let { a -> state.quota.entries.firstOrNull { it.key.equals(a, ignoreCase = true) }?.value }
                ?: Accounts.personalQuota(state)?.let { state.quota[it] })?.takeUnless { it.stale }?.w7?.let { "$it%" } ?: NONE
            Metric.WORKING -> counts.working.toString()
            Metric.WAITING -> counts.waiting.toString()
            Metric.IDLE -> counts.idle.toString()
            Metric.NIGHT -> state.night.queued.toString()
            Metric.CONTEXT -> scope.singleOrNull()?.context?.let { "$it%" } ?: NONE
            Metric.TURN_AGE -> scope.singleOrNull()?.let { s -> Durations.since(s.turnStarted ?: s.since, now) } ?: NONE
            Metric.OUTCOME -> scope.singleOrNull()?.outcome?.short ?: NONE
        }
    }
}
