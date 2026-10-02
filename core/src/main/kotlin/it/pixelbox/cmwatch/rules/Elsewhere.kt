package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * L'avviso delle altre sessioni nella chat di una sessione (Franz, 02/10 20:47, variante A dei mockup): una pastiglia
 * sotto la barra. Prima chi ti aspetta, finché la domanda c'è; poi un turno finito, una volta sola e solo se recente,
 * delle sessioni che segui o a cui hai scritto dal telefono (con 5-6 sessioni al lavoro, tutte non sarebbe discreto).
 * Mai la sessione aperta.
 */
object Elsewhere {
    sealed interface Alert { val key: String }

    /** Le sessioni ferme su una domanda, dalla più vecchia. */
    data class Waiting(val sessions: List<String>) : Alert { override val key get() = "w:" + sessions.joinToString(",") }

    /** Il turno finito più recente non ancora visto. */
    data class Finished(val session: String, val at: Long) : Alert { override val key get() = "f:$session@$at" }

    /** Oltre questo tempo un esito non è più una novità: all'apertura dell'app non si elencano i turni di ore fa. */
    const val FRESH_S = 600L

    fun alert(state: State, current: String, now: Long, mine: Set<String>, seen: Set<String>): Alert? {
        val others = state.sessions.filter { it.name != current && it.state != SessionState.GONE }
        val waiting = others.filter { it.question != null }.sortedBy { it.question!!.askedAt }.map { it.name }
        if (waiting.isNotEmpty()) return Waiting(waiting)
        return others.filter { it.state == SessionState.IDLE && (it.followed || it.name in mine) }
            .mapNotNull { s -> s.outcome?.takeIf { now - it.at <= FRESH_S }?.let { Finished(s.name, it.at) } }
            .filter { it.key !in seen }
            .maxByOrNull { it.at }
    }
}
