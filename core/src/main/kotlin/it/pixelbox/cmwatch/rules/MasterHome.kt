package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
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
    /** Il resoconto della notte resta fino a mezzogiorno; la notte si propone dalle 20. */
    const val MORNING_END = 12
    const val EVENING = 20

    fun forYou(
        state: State, events: List<Event>, sent: List<Sent>, now: Long, zone: ZoneId, read: Set<String> = emptySet(),
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
        return ForYou(all.take(MAX), (all.size - MAX).coerceAtLeast(0))
    }
}
