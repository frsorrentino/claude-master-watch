package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.TranscriptEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * «Per te» nella casa della master (design 01/10, approvato da Franz alle 23:02): le cose che chiedono Franz, una riga e
 * un tasto ciascuna, al massimo [MAX] più il conto delle altre. Ordine per urgenza: domande, turni finiti (variante 3,
 * 02/10 21:11), contesto dall'80 %, resoconto della notte (al mattino, finché non è letto), notte (dalle 20), prossimi passi
 * del recap dei progetti senza sessione, invii programmati. Le righe sono dati: le frasi le compone l'app dalle sue stringhe.
 */
object MasterHome {
    enum class Kind { QUESTION, FINISHED, CONTEXT, NIGHT_REPORT, NIGHT, NEXT_STEP, SCHEDULED }

    /**
     * `title`/`detail`: nome e testo (sessione e domanda, progetto e passo, titolo e corpo del resoconto); `number`: la
     * percentuale del contesto, i lavori in coda o gli invii programmati; `at`: il primo invio programmato; `key`: la
     * chiave dell'evento, per segnarlo letto; `project`: la cartella da lanciare per un prossimo passo senza sessione.
     */
    data class Row(
        val kind: Kind, val title: String = "", val detail: String? = null, val session: String? = null,
        val project: String? = null, val key: String? = null, val number: Int? = null, val at: Long? = null,
    )

    data class ForYou(val rows: List<Row>, val more: Int)

    const val MAX = 3

    /**
     * Con una domanda aperta «Per te» va prima dell'ultimo esito (consulenza del 02/10, approvata da Franz il 03/10): il
     * lavoro bloccato in vista senza scorrere.
     */
    fun forYouFirst(f: ForYou): Boolean = f.rows.any { it.kind == Kind.QUESTION }

    /** La chiave di un prossimo passo avviato dal telefono: ricordata, il passo non torna (revisione finale 02/10). */
    fun nextKey(project: String, next: String) = "next:$project:$next"

    /**
     * Variante 3 dei mockup (Franz, 02/10 21:11): chi ha finito il turno resta in «Per te» finché non lo apri, non gli
     * scrivi o non passano [FINISHED_S]; come l'avviso nella chat, solo le sessioni seguite o a cui hai scritto dal telefono.
     */
    fun finishedKey(session: String, at: Long) = "finished:$session@$at"
    const val FINISHED_S = 12 * 3600L
    /** Il resoconto della notte resta fino a mezzogiorno; la notte si propone dalle 20. */
    const val MORNING_END = 12
    const val EVENING = 20

    fun forYou(
        state: State, events: List<Event>, sent: List<Sent>, now: Long, zone: ZoneId, read: Set<String> = emptySet(),
        /** Quante righe tenere: [MAX] nella card, tutte quando si apre «+N». */
        limit: Int = MAX,
    ): ForYou {
        val local = Instant.ofEpochSecond(now).atZone(zone)
        val today = local.toLocalDate()
        val live = state.sessions.filter { it.state != SessionState.GONE }
        val all = buildList {
            live.mapNotNull { s -> s.question?.let { q -> Triple(s, q, q.askedAt) } }.sortedBy { it.third }
                .forEach { (s, q, _) -> add(Row(Kind.QUESTION, s.name, q.text, s.name, at = q.askedAt)) }
            live.filter { it.name != ContextActions.MASTER && it.state == SessionState.IDLE }
                .mapNotNull { s -> s.outcome?.let { o -> s to o } }
                .filter { (s, o) ->
                    now - o.at <= FINISHED_S && finishedKey(s.name, o.at) !in read &&
                        (s.followed || sent.any { it.session == s.name }) && sent.none { it.session == s.name && it.sentAt > o.at }
                }
                .sortedByDescending { it.second.at }
                .forEach { (s, o) -> add(Row(Kind.FINISHED, s.name, o.full, s.name, key = finishedKey(s.name, o.at), at = o.at)) }
            live.filter { ContextActions.urgent(it.context) }.sortedByDescending { it.context }
                .forEach { s -> add(Row(Kind.CONTEXT, s.name, session = s.name, number = s.context)) }
            if (local.hour < MORNING_END) events.filter { it.kind == EventKind.NIGHT_REPORT }.maxByOrNull { it.ts }
                ?.takeIf { e -> Instant.ofEpochSecond(e.ts).atZone(zone).toLocalDate() == today && e.key !in read }
                ?.let { e -> add(Row(Kind.NIGHT_REPORT, e.title, e.body, key = e.key)) }
            if (local.hour >= EVENING && state.night.items != null) add(Row(Kind.NIGHT, number = state.night.queued))
            // Il recap di oggi o di ieri: quello delle 20 serve anche la mattina dopo.
            val recapDay = runCatching { LocalDate.parse(state.recap.date) }.getOrNull()
            if (recapDay != null && !recapDay.isBefore(today.minusDays(1))) state.recap.items.forEach { item ->
                val next = item.next?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
                // Mai a una sessione già aperta (Franz, 02/10 20:49: «Avvia» sembrava riaprirla): quella sta fra chi ha finito.
                if (live.any { it.project == item.project }) return@forEach
                // Già avviato: ricordato dall'app.
                if (nextKey(item.project, next) in read) return@forEach
                add(Row(Kind.NEXT_STEP, item.project, next, project = state.projects.firstOrNull { it.name == item.project }?.path))
            }
            val waiting = sent.filter(ChatRules::waiting)
            waiting.minByOrNull { it.scheduledFor!! }?.let { first ->
                add(Row(Kind.SCHEDULED, session = first.session, number = waiting.size, at = first.scheduledFor))
            }
        }
        return ForYou(all.take(limit), (all.size - limit).coerceAtLeast(0))
    }

    /** L'ultima risposta della master in testa alla casa (casa A, 02/10): titolo, resto del testo, consigli, ora. */
    data class Hero(val headline: String, val body: String, val steps: List<String>, val at: Long?)

    /** Una sessione in corso, una riga: la domanda, lo strumento al lavoro o l'ultimo esito. */
    data class Running(val session: Session, val detail: String?)

    private const val OUTCOME = "Esito:"
    private const val WATCH = "Watch:"

    /**
     * Dalla conversazione l'ultima risposta di Claude; senza conversazione l'esito che il relay riporta. Il titolo è la
     * riga «Esito:», se c'è, se no la prima riga; la riga per l'orologio non si vede.
     */
    fun hero(entries: List<TranscriptEntry>, master: Session): Hero? {
        val last = entries.lastOrNull { it.role != "user" && it.role != "tool" && !it.text.isNullOrBlank() }
        val raw = last?.text ?: master.outcome?.full ?: return null
        val parsed = NextSteps.parse(raw)
        val lines = parsed.text.split('\n').filterNot { it.trimStart().startsWith(WATCH) }
        val outcome = lines.indexOfFirst { it.trimStart().startsWith(OUTCOME) }
        val head = if (outcome >= 0) outcome else lines.indexOfFirst { it.isNotBlank() }
        if (head < 0) return null
        val headline = lines[head].trim().removePrefix(OUTCOME).trim()
        val body = lines.filterIndexed { i, _ -> i != head }.joinToString("\n").trim()
        return Hero(headline, body, parsed.steps, last?.at ?: master.outcome?.at)
    }

    /** Le sessioni vive senza la master, nell'ordine della regia (in attesa, al lavoro, ferme). */
    /**
     * Il gruppo «al lavoro» dentro «Per te» (Franz, 02/10 21:29: «In corso» integrato): solo chi lavora adesso; chi aspetta
     * o ha finito sta già nelle righe sopra, la master ha la sua casa. `detail` è l'ultimo esito intero, con la riga
     * `Prossimi:` per le risposte rapide, non il comando che gira (Franz, 03/10 09:15).
     */
    fun working(state: State): List<Running> =
        running(state).filter { it.session.state == SessionState.BUSY || it.session.state == SessionState.AWAITING }
            .map { it.copy(detail = it.session.outcome?.full) }

    fun running(state: State): List<Running> =
        PhoneBoard.sections(state, withMaster = false).filter { it.group != PhoneBoard.Group.CLOSED }.flatMap { it.sessions }.map { s ->
            val detail = when (s.state) {
                SessionState.WAITING -> s.question?.text
                SessionState.BUSY, SessionState.AWAITING -> listOfNotNull(s.tool, s.toolNote).joinToString(" · ").ifEmpty { null }
                else -> s.outcome?.short
            }
            Running(s, detail)
        }
}
