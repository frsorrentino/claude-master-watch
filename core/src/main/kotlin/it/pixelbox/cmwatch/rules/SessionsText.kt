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
    data class Cell(val title: String?, val detail: String?) {
        /**
         * Quattro righe in tutto (Franz, 14/09 08:10). Senza il prossimo passo sotto, il titolo le prende tutte: a 42
         * caratteri, la misura della tile, «- Passkey Google: il login…» diventava «- Passkey Google» (15/09 08:30).
         */
        val titleLines: Int get() = if (detail.isNullOrBlank()) 4 else 2

        /** Il titolo nelle sue righe, un pensiero intero e mai «…»: una riga ne tiene circa 22 caratteri. */
        val titleText: String? get() = title?.let { TileTexts.fitTile(it, max = if (titleLines == 4) TITLE_MAX_4 else TileTexts.TILE_MAX) }

        /** La card dice già `text`: la testata della scheda allora non lo ripete. */
        fun says(text: String?): Boolean = repeats(text, title) || repeats(text, detail)
    }

    /**
     * Due testi dicono la stessa cosa se, senza maiuscole e punteggiatura finale, uno comincia con l'altro a confine di
     * parola: la testata taglia la description a 56 caratteri, la card la mostra intera. Tre testi sulla scheda devono
     * dire tre cose diverse, altrimenti resta solo quello completo (Franz, 15/09 16:00).
     */
    fun repeats(a: String?, b: String?): Boolean {
        val x = norm(a ?: return false)
        val y = norm(b ?: return false)
        if (x.isEmpty() || y.isEmpty()) return false
        val (corto, lungo) = if (x.length <= y.length) x to y else y to x
        return lungo.startsWith(corto) && (lungo.length == corto.length || !lungo[corto.length].isLetterOrDigit())
    }

    private fun norm(t: String) = TileTexts.plain(t).lowercase().replace(Regex("\\s+"), " ").trim().trimEnd('.', '…', ':', ';', ',', ' ')

    /**
     * La card della Scheda, che ora contiene anche l'Esito (Franz, 15/09 17:02: tre schermate fuse in due). Chi è fermo
     * mostra l'esito intero, titolo e resto sotto; chi lavora, domanda o è senza esito resta come la cella della lista.
     * Il prossimo passo sotto solo se non ripete l'esito.
     */
    data class Sheet(val title: String?, val body: String?, val detail: String?) {
        fun says(text: String?): Boolean = repeats(text, title) || repeats(text, body) || repeats(text, detail)
    }

    fun sheet(
        s: Session,
        now: Long,
        running: String,
        idle: String,
        tools: ToolText.Labels? = null,
        zone: ZoneId = ZoneId.systemDefault(),
        live: String? = null,
    ): Sheet {
        val c = cell(s, now, running, idle, tools, zone)
        // Chi lavora senza uno strumento in vista diceva solo «turno in corso» (Franz, 15/09 19:01): il titolo diventa
        // l'ultimo blocco del suo terminale, se il PC l'ha mandato, e sotto il suo ultimo esito.
        val lavora = s.question == null && (s.state == SessionState.BUSY || s.state == SessionState.AWAITING)
        if (lavora && s.tool.isNullOrBlank()) {
            val t = live?.trim()?.takeIf { it.isNotEmpty() } ?: c.title
            val esito = s.outcome?.short?.let { TileTexts.plain(it) }?.takeIf { it.isNotEmpty() }
            return Sheet(t, null, listOfNotNull(esito, c.detail).firstOrNull { !repeats(it, t) })
        }
        val o = s.outcome
        val ferma = s.question == null && s.state != SessionState.BUSY && s.state != SessionState.AWAITING
        if (!ferma || o == null) return Sheet(c.title, null, c.detail)
        val title = OutcomeText.headline(o)
        val body = OutcomeText.cardBody(o)
        val detail = c.detail?.takeUnless { d -> repeats(d, title) || repeats(d, body) || body?.contains(d.trim()) == true }
        return Sheet(title, body, detail)
    }

    /** Titolo e dettaglio che ripetono la stessa cosa: resta il più lungo, da solo, con tutte le righe per sé. */
    private fun pair(title: String?, detail: String?): Cell =
        if (repeats(title, detail)) Cell(title = listOfNotNull(title, detail).maxBy { it.length }, detail = null) else Cell(title, detail)

    const val TITLE_MAX_4 = 88

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
        val esito = s.outcome?.short?.let { TileTexts.plain(it) }?.takeIf { it.isNotEmpty() }
        return when {
            // Chi aspetta: la domanda occupa il posto d'onore, il resto lo dice la schermata.
            s.question != null -> Cell(title = s.question?.text, detail = null)
            // Una chiusa ha comunque una cosa da dire: l'ultima che ha fatto, altrimenti il suo progetto (Franz,
            // 14/09 08:00: «ci sarebbe lo spazio per un'altra riga»).
            s.state == SessionState.GONE -> Cell(title = esito ?: s.project, detail = null)
            s.state == SessionState.BUSY || s.state == SessionState.AWAITING -> {
                val t = TileTexts.activity(f, busy = true, running = running, idle = idle, now = now, tools = tools)
                pair(t, next)
            }
            else -> pair(esito ?: next ?: idle, next)
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
