package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.TimelineEvent
import it.pixelbox.cmwatch.contract.TimelinePage
import java.time.Instant
import java.time.ZoneId

/**
 * Il tablet (piano 04/10, mockup approvati da Franz): la stessa app, sugli schermi larghi la plancia (riga di stato,
 * barra di navigazione, sessioni con le quote, conversazione, ispettore) e le colonne. Qui i conti, la UI in `:mobile`.
 */
object Tablet {
    /** Da qui la plancia: tablet in orizzontale, finestra larga del Chromebook. Sotto resta l'app del telefono. */
    const val WIDE_DP = 840

    /** Da qui anche l'ispettore a destra: sotto, la conversazione in mezzo resterebbe troppo stretta. */
    const val INSPECTOR_DP = 1200

    fun wide(widthDp: Int) = widthDp >= WIDE_DP
    fun inspector(widthDp: Int) = widthDp >= INSPECTOR_DP

    /** La quota di un account nella riga di stato: 5h con la ripartenza, 7g. */
    data class Account(val account: String, val personal: Boolean, val h5: Int?, val resetAt: Long?, val w7: Int?, val stale: Boolean)

    /** La riga di stato in alto: PC, aggiornamento, quote, sessioni per stato (la master compresa), notte. */
    data class Status(
        val host: String, val minutes: Int, val stale: Boolean, val accounts: List<Account>,
        val open: Int, val working: Int, val finished: Int, val waiting: Int, val closed: Int, val night: Int,
    )

    fun status(state: State, summary: Summary.Model, now: Long, stale: Boolean): Status {
        val g = groups(summary).associate { it.first to it.second.size }
        val accounts = PhoneBoard.quotaRows(state, now, stale).map { r ->
            Account(r.account, r.personal, r.pct, r.resetAt, state.quota[r.account]?.w7, r.stale)
        }
        return Status(
            state.host, ((now - state.ts) / 60).toInt().coerceAtLeast(0), stale, accounts,
            open = state.sessions.count { it.state != SessionState.GONE },
            working = g[Summary.Group.WORKING] ?: 0, finished = g[Summary.Group.FINISHED] ?: 0,
            waiting = g[Summary.Group.WAITING] ?: 0, closed = summary.closed.size, night = state.night.queued,
        )
    }

    /**
     * La colonna delle sessioni: i gruppi del riepilogo nell'ordine del bisogno, con la master al suo posto come le altre
     * (sul telefono è la cornice della home, qui una sessione della lista). Fra le ferme vale l'ordine del riepilogo, la
     * più recente in alto.
     */
    fun groups(summary: Summary.Model): List<Pair<Summary.Group, List<Summary.Row>>> {
        val m = summary.master
        val rows = if (m == null || summary.rows.any { it.session.name == m.name }) summary.rows else {
            val group = if (m.state == SessionState.BUSY || m.state == SessionState.AWAITING) Summary.Group.WORKING else Summary.Group.STILL
            summary.rows + Summary.Row(group, m, m.outcome?.full, if (group == Summary.Group.WORKING) m.turnStarted ?: m.since else m.since)
        }
        return Summary.Group.entries.mapNotNull { g ->
            val inGroup = rows.filter { it.group == g }.let { r -> if (g == Summary.Group.STILL) r.sortedByDescending { it.session.since } else r }
            inGroup.takeIf { it.isNotEmpty() }?.let { g to it }
        }
    }

    /** La riga di attività sotto il nome: la domanda, lo strumento al lavoro, l'esito; null se non c'è niente da dire. */
    fun line(row: Summary.Row): String? = when (row.group) {
        Summary.Group.WAITING -> row.session.question?.text
        Summary.Group.WORKING -> listOfNotNull(row.session.tool, row.session.toolNote).joinToString(" · ").ifEmpty { null }
        Summary.Group.FINISHED -> row.text ?: row.session.outcome?.short
        Summary.Group.STILL -> row.session.outcome?.short
    }?.lineSequence()?.firstOrNull { it.isNotBlank() }?.trim()

    /**
     * L'ispettore a destra: aperta da, da quanto, il turno in corso, e la giornata dalla cronologia (contratto 1.29) della
     * sessione viva. `commits` e `prompts` null finché la cronologia non c'è: un numero inventato direbbe zero.
     */
    data class Inspector(
        val session: Session, val openedAt: Long, val openFor: Long, val turn: Long?,
        val today: List<TimelineEvent>, val commits: Int?, val prompts: Int?,
    )

    fun inspect(session: Session, page: TimelinePage?, now: Long, zone: ZoneId): Inspector {
        val midnight = midnight(now, zone)
        val mine = page?.sessions?.filter { it.session == session.name && it.live }
        val today = mine?.flatMap { it.events }?.filter { it.at >= midnight }?.sortedBy { it.at }.orEmpty()
        val turn = session.turnStarted?.takeIf { session.state == SessionState.BUSY || session.state == SessionState.AWAITING }?.let { (now - it).coerceAtLeast(0) }
        return Inspector(
            session, session.since, (now - session.since).coerceAtLeast(0), turn, today,
            commits = mine?.let { today.count { it.kind == "commit" } }, prompts = mine?.let { today.count { it.kind == "prompt" } },
        )
    }

    /** L'argomento di `timeline`: da mezzanotte di oggi, come epoch (contratto 1.29 accetta un epoch). */
    fun timelineArg(now: Long, zone: ZoneId): String = midnight(now, zone).toString()

    private fun midnight(now: Long, zone: ZoneId): Long =
        Instant.ofEpochSecond(now).atZone(zone).toLocalDate().atStartOfDay(zone).toEpochSecond()

    /**
     * Il grafico della quota 5h: la finestra da 5 ore prima della ripartenza alla ripartenza, x da 0 a 1. `points` i campioni
     * (linea piena), `nowX` dove si è adesso, `projected` dove si arriva al ritmo attuale (linea tratteggiata), se si può dire.
     */
    data class Forecast(val start: Long, val resetAt: Long, val points: List<Pair<Float, Int>>, val nowX: Float, val projected: Int?)

    fun forecast(pace: QuotaHistory.Pace, resetAt: Long, now: Long): Forecast {
        val start = resetAt - QuotaHistory.WINDOW_S
        val x = { t: Long -> ((t - start).toFloat() / QuotaHistory.WINDOW_S).coerceIn(0f, 1f) }
        return Forecast(start, resetAt, pace.points.map { x(it.ts) to it.pct }, x(now), pace.projected)
    }

    /**
     * La settimana senza storico per giorno (nessun campione lo tiene): il ritmo medio da quando è ripartita, esteso fino al
     * rinnovo, al massimo 100. Nella prima ora dopo la ripartenza il ritmo non si dice.
     */
    fun weekProjected(w7: Int?, resetAt: Long?, now: Long): Int? {
        if (w7 == null || resetAt == null || resetAt <= now) return null
        val elapsed = now - (resetAt - WEEK_S)
        if (elapsed < 3600) return null
        return (w7 + w7.toDouble() / elapsed * (resetAt - now)).toInt().coerceIn(0, 100)
    }

    /** Pezzo 5: le colonne affiancate, al massimo quattro. */
    const val MAX_COLUMNS = 4

    /**
     * Le sessioni in colonna: quelle scelte ancora vive, nel loro ordine, fino a quattro. Senza una scelta salvata (null) le
     * prime tre di `live`; una scelta vuota resta vuota (tolte tutte a mano).
     */
    fun columns(pinned: List<String>?, live: List<String>): List<String> =
        pinned?.filter { it in live }?.distinct()?.take(MAX_COLUMNS) ?: live.take(3)

    /** Tocco su una sessione della barra: entra in fondo o esce; con quattro colonne prende il posto dell'ultima. */
    fun toggle(columns: List<String>, name: String): List<String> = when {
        name in columns -> columns - name
        columns.size < MAX_COLUMNS -> columns + name
        else -> columns.dropLast(1) + name
    }

    fun columnsPref(columns: List<String>): String = columns.joinToString("\n")
    fun columnsFromPref(raw: String?): List<String>? = raw?.split('\n')?.filter { it.isNotBlank() }

    private const val WEEK_S = 7L * 86400
}
