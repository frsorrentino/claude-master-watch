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

    /** v2 (Franz, 04/10 16:36): il tocco su una scheda aggiunge; già in colonna niente; con quattro prende il posto dell'ultima. */
    fun add(columns: List<String>, name: String): List<String> = when {
        name in columns -> columns
        columns.size < MAX_COLUMNS -> columns + name
        else -> columns.dropLast(1) + name
    }

    /** Una colonna trascinata sopra un'altra: si scambiano di posto. */
    fun swap(columns: List<String>, from: Int, to: Int): List<String> {
        if (from == to || from !in columns.indices || to !in columns.indices) return columns
        return columns.toMutableList().also { it[from] = columns[to]; it[to] = columns[from] }
    }

    /**
     * Le larghezze delle colonne a scatti (Franz, 04/10 16:21 e 16:36): 12 parti in tutto, ogni colonna almeno 2 (un sesto).
     * Un bordo trascinato sposta parti intere fra le due colonne che separa: così una o due possono prendere metà o due
     * terzi dello spazio e le altre dividersi il resto.
     */
    object Shares {
        const val TOTAL = 12
        const val MIN = 2

        fun equal(n: Int): List<Int> = if (n <= 0) emptyList() else List(n) { i -> TOTAL / n + if (i < TOTAL % n) 1 else 0 }

        fun drag(shares: List<Int>, border: Int, parts: Int): List<Int> {
            if (border !in 0 until shares.size - 1) return shares
            val pair = shares[border] + shares[border + 1]
            val left = (shares[border] + parts).coerceIn(MIN, pair - MIN)
            return shares.toMutableList().also { it[border] = left; it[border + 1] = pair - left }
        }

        /** I pixel trascinati in parti intere della larghezza delle colonne (lo scatto). */
        fun parts(dragPx: Float, widthPx: Float): Int = if (widthPx <= 0f) 0 else kotlin.math.round(dragPx / (widthPx / TOTAL)).toInt()

        fun pref(shares: List<Int>): String = shares.joinToString(",")

        fun fromPref(raw: String?, n: Int): List<Int> {
            val s = raw?.split(',')?.mapNotNull { it.trim().toIntOrNull() }
            return s?.takeIf { it.size == n && it.sum() == TOTAL && it.all { p -> p >= MIN } } ?: equal(n)
        }
    }

    fun columnsPref(columns: List<String>): String = columns.joinToString("\n")
    fun columnsFromPref(raw: String?): List<String>? = raw?.split('\n')?.filter { it.isNotBlank() }

    private const val WEEK_S = 7L * 86400
}
