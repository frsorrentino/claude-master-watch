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
 * un tasto ciascuna, al massimo [MAX] più il conto delle altre. Ordine per urgenza: domande, contesto dall'80 %, resoconto
 * della notte (al mattino, finché non è letto), notte (dalle 20), prossimi passi del recap, invii programmati. Le righe
 * sono dati: le frasi le compone l'app dalle sue stringhe.
 */
object MasterHome {
    enum class Kind { QUESTION, CONTEXT, NIGHT_REPORT, NIGHT, NEXT_STEP, SCHEDULED }

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

    /** La chiave di un prossimo passo avviato dal telefono: ricordata, il passo non torna (revisione finale 02/10). */
    fun nextKey(project: String, next: String) = "next:$project:$next"
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
                .forEach { (s, q, _) -> add(Row(Kind.QUESTION, s.name, q.text, s.name)) }
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
                val s = live.firstOrNull { it.project == item.project }
                // Già avviato: ricordato dall'app, o già mandato a quella sessione come prompt.
                if (nextKey(item.project, next) in read) return@forEach
                if (s != null && sent.any { it.session == s.name && it.text.trim() == next }) return@forEach
                when {
                    s == null -> add(Row(Kind.NEXT_STEP, item.project, next, project = state.projects.firstOrNull { it.name == item.project }?.path))
                    s.state == SessionState.IDLE -> add(Row(Kind.NEXT_STEP, item.project, next, session = s.name))
                }
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
