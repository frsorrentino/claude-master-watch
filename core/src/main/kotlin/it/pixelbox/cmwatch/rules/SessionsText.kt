package it.pixelbox.cmwatch.rules

import java.time.Instant
import java.time.ZoneId
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState

/** Testi puri della lista Sessioni: icona e pallino dell'account sono composable, qui solo «nome · durata». */
object SessionsText {
    fun row(s: Session, now: Long): String {
        val from = when (s.state) {
            SessionState.WAITING -> s.question?.askedAt ?: s.since
            SessionState.BUSY -> s.turnStarted ?: s.since
            SessionState.GONE -> return s.name
            else -> s.since
        }
        return "${s.name} · ${Durations.since(from, now)}"
    }

    /**
     * Sottotitolo della riga nella lista. Per una sessione finita dice a parole che è chiusa e da quando non si
     * vede: il badge grigio con la ✗ da solo non diceva se fosse aperta o chiusa (Franz, 13/09 18:01).
     */
    fun sub(s: Session, now: Long, closed: String): String? = when (s.state) {
        SessionState.GONE -> "$closed · ${Durations.since(s.since, now)}"
        SessionState.WAITING -> Durations.since(s.question?.askedAt ?: s.since, now)
        SessionState.BUSY, SessionState.AWAITING -> Durations.since(s.turnStarted ?: s.since, now)
        SessionState.IDLE -> Durations.since(s.since, now)
    }

    /**
     * Contenuto della cella nella lista, misurata sulle celle di notifica di Wear OS (Franz, 13/09 21:32): riga del
     * nome, poi un titolo chiaro su cosa sta facendo e un dettaglio grigio su cosa segue. Niente testo inventato: se
     * un campo non c'è, la riga non si disegna.
     */
    data class Cell(val title: String?, val detail: String?)

    fun cell(
        s: Session,
        now: Long,
        running: String,
        idle: String,
        tools: ToolText.Labels? = null,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Cell {
        // `next_at` è la mezzanotte del giorno della riga di recap: una riga che non è di oggi è stantia e la scheda la
        // tace, anche come titolo di ripiego (Franz, 14/09 10:42: «anche queste info sono stantie»).
        val oggi = Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
        val fresca = s.nextAt?.let { Instant.ofEpochSecond(it).atZone(zone).toLocalDate() == oggi } == true
        val f = if (fresca) s else s.copy(next = null, nextAt = null)
        val next = f.next?.trim()?.takeIf { it.isNotEmpty() && f.nextAt != null }
        val esito = s.outcome?.short?.trim()?.takeIf { it.isNotEmpty() }
        return when {
            // Chi aspetta: la domanda occupa il posto d'onore, il resto lo dice la schermata.
            s.question != null -> Cell(title = s.question?.text, detail = null)
            // Una chiusa ha comunque una cosa da dire: l'ultima che ha fatto, altrimenti il suo progetto (Franz,
            // 14/09 08:00: «ci sarebbe lo spazio per un'altra riga»).
            s.state == SessionState.GONE -> Cell(title = esito ?: s.project, detail = null)
            s.state == SessionState.BUSY || s.state == SessionState.AWAITING -> {
                val t = TileTexts.activity(f, busy = true, running = running, idle = idle, now = now, tools = tools)
                Cell(title = t, detail = next?.takeIf { it != t })
            }
            else -> Cell(title = esito ?: next ?: idle, detail = next?.takeIf { it != esito })
        }
    }

    fun header(list: List<Session>, sessionsLabel: String): String {
        val q = list.count { it.question != null }
        val g = list.count { it.state == SessionState.GONE }
        val parts = mutableListOf("${list.size} $sessionsLabel")
        if (q > 0) parts += "$q ❓"
        if (g > 0) parts += "$g ✗"
        return parts.joinToString(" · ")
    }
}
