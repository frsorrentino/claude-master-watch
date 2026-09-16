package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * La sezione Lavoro della pagina Quota rifatta (Franz, 16/09 13:00, «ok tutte»). Prima erano quattro card uguali —
 * numero, pillola, anello — e due non dicevano niente: l'anello delle sessioni attive su quelle vive e la card
 * «Aggiornato». Qui i numeri per una grafica per dato: la barra a segmenti di «Adesso», le barrette del contesto, le
 * domande e la notte solo quando ci sono, e la riga dell'aggiornamento in fondo. «Oggi» e il ritmo della finestra hanno
 * le loro regole (`DayBars`, `QuotaHistory`).
 */
object WorkPanel {
    /** Il segmento di una sessione viva, nei colori dei badge. */
    enum class Seg { WAITING, WORKING, IDLE }

    data class Now(val segments: List<Seg>, val working: Int, val waiting: Int, val idle: Int)

    /** Prima chi aspetta te, poi chi lavora, poi chi è ferma: si legge da sinistra quello che chiede attenzione. */
    fun now(state: State): Now {
        val segs = state.sessions.mapNotNull { s ->
            when {
                s.state == SessionState.GONE -> null
                s.question != null || s.state == SessionState.WAITING -> Seg.WAITING
                s.state == SessionState.BUSY || s.state == SessionState.AWAITING -> Seg.WORKING
                else -> Seg.IDLE
            }
        }.sortedBy { it.ordinal }
        return Now(segs, segs.count { it == Seg.WORKING }, segs.count { it == Seg.WAITING }, segs.count { it == Seg.IDLE })
    }

    data class Ctx(val name: String, val pct: Int, val tone: BriefCards.Tone)

    /** Quante righe entrano nella card senza farla diventare una lista: le più piene sono quelle da guardare. */
    const val MAX_CONTEXT_ROWS = 4

    /** Le sessioni vive con il contesto noto, dalla più piena, con le soglie della card delle misure. */
    fun contexts(state: State): List<Ctx> = state.sessions
        .filter { it.state != SessionState.GONE && it.context != null }
        .sortedByDescending { it.context }
        .take(MAX_CONTEXT_ROWS)
        .map { Ctx(it.name, it.context!!, SessionMeters.contextTone(it.context)) }

    data class Questions(val count: Int, val oldest: String, val age: String)

    /** Null senza domande: la card compare solo quando c'è qualcuno che aspetta. */
    fun questions(state: State, now: Long): Questions? {
        val con = state.sessions.filter { it.question != null }
        val vecchia = con.minByOrNull { it.question!!.askedAt } ?: return null
        return Questions(con.size, vecchia.name, Durations.since(vecchia.question!!.askedAt, now))
    }

    /** `progress`: quanta coda è già passata, una in corso su quelle che restano; zero se non ne gira nessuna. */
    data class NightQueue(val queued: Int, val running: String?, val progress: Float)

    fun night(state: State): NightQueue? {
        val n = state.night
        if (n.queued <= 0 && n.running == null) return null
        return NightQueue(n.queued, n.running, if (n.running == null) 0f else 1f / (n.queued + 1))
    }

    data class Updated(val minutes: Int, val host: String, val stale: Boolean)

    /** Età dello stato in minuti e nome della macchina, per la riga in fondo; `stale` la fa diventare rossa. */
    fun updated(state: State, freshness: Freshness, now: Long): Updated =
        Updated(((now - state.ts) / 60).toInt().coerceAtLeast(0), state.host, freshness is Freshness.Stale)
}
