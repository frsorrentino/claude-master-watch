package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.NightEntry
import it.pixelbox.cmwatch.contract.NightReport
import it.pixelbox.cmwatch.contract.Option
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * La pagina «Notte» dal rapporto (specifica docs/proposte/2026-10-07-pagina-notte.md, approvata da Franz il 07/10 alle
 * 21:50): solo i conti, cioè icona, titolo, posizione sull'asse della finestra, passi e progetti. Date e durate a parole le
 * scrive ogni schermata con le sue risorse. Gli stessi casi sono in web/src/lib/night.test.ts.
 */
object NightPage {
    /** Gli stessi significati di Telegram: ✓ ok, ✗ fermo o fallito, ▶ in corso, ❓ domanda aperta. */
    enum class Icon { OK, STOPPED, RUNNING, QUESTION }
    enum class NeedKind { QUESTION, APPROVAL, UNBLOCK }

    /** Una barra più corta di così non si vedrebbe: un lavoro di un minuto resta un punto. */
    const val MIN_BAR = 0.012

    /** Quante voci per esito, per le pillole in testa (Franz, 08/10 12:30: «non capisco cosa è da fare o fatto»). */
    data class Counts(val done: Int, val running: Int, val stopped: Int, val asking: Int)
    data class Page(
        val day: LocalDate, val dayBefore: LocalDate, val start: Long, val end: Long, val windowS: Int,
        val fromLastMessage: Boolean, val needs: List<Need>, val cards: List<Card>, val axis: List<Tick>, val projects: List<Project>, val counts: Counts = Counts(0, 0, 0, 0),
    )
    data class Need(val kind: NeedKind, val session: String, val text: String, val options: List<Option> = emptyList(), val task: String? = null)
    data class Tick(val label: String, val at: Double)
    data class Step(val at: Long, val kind: String, val text: String, val ok: Boolean?)
    data class Card(
        val id: String, val icon: Icon, val title: String, val folder: String?, val queue: Boolean,
        val start: Long, val end: Long?, val durationS: Int?, val detail: String?, val from: Double, val to: Double,
        /** La sessione viva da aprire con «Conversazione»; null se non c'è (chiusa o lavoro della coda: richiesta 2). */
        val chat: String?, val prompts: Int?, val tests: Int?, val commits: Int?, val steps: List<Step>,
    )
    data class Project(val name: String, val done: Int, val total: Int, val parts: List<String>, val waiting: List<String>, val next: String?)

    fun of(r: NightReport, zone: ZoneId): Page {
        val start = r.window.start
        val end = maxOf(r.window.end, start + 60)
        val span = (end - start).toDouble()
        fun frac(t: Long) = ((t - start) / span).coerceIn(0.0, 1.0)
        val asking = r.attention.questions.map { it.session }.toSet()
        val cards = r.timeline.map { i ->
            val queue = i.kind == NightEntry.Kind.NIGHT_JOB
            val (folder, rest) = if (queue && i.title.contains(" — ")) i.title.substringBefore(" — ") to i.title.substringAfter(" — ") else null to i.title
            var from = frac(i.start)
            var to = frac(i.end ?: end)
            if (to - from < MIN_BAR) { to = minOf(1.0, from + MIN_BAR); from = maxOf(0.0, to - MIN_BAR) }
            val project = r.projects.firstOrNull { p -> p.path != null && p.path == i.project }
            val until = (i.end ?: end) + 60
            Card(
                id = i.id,
                icon = when {
                    i.kind == NightEntry.Kind.SESSION && i.id in asking -> Icon.QUESTION
                    i.outcome == "ok" -> Icon.OK
                    i.outcome == "running" -> Icon.RUNNING
                    else -> Icon.STOPPED
                },
                title = if (queue) shortTitle(rest) else i.title,
                folder = folder, queue = queue, start = i.start, end = i.end,
                durationS = i.end?.let { e -> (e - i.start).toInt().coerceAtLeast(0) },
                detail = i.detail, from = from, to = to,
                chat = i.id.takeIf { i.kind == NightEntry.Kind.SESSION && i.live },
                prompts = i.counts?.prompts, tests = i.counts?.tests, commits = i.counts?.commits,
                steps = project?.events.orEmpty().filter { e -> e.at >= i.start - 60 && e.at <= until }.map { e -> Step(e.at, e.kind, e.text, e.ok) },
            )
        }
        val hm = DateTimeFormatter.ofPattern("HH:mm")
        val axis = generateSequence(((start / 1800) + 1) * 1800) { it + 1800 }.takeWhile { it < end }
            .map { t -> Tick(hm.format(Instant.ofEpochSecond(t).atZone(zone)), frac(t)) }.toList()
        val needs = r.attention.questions.map { q -> Need(NeedKind.QUESTION, q.session, q.text, q.options) } +
            r.attention.approvals.map { a -> Need(NeedKind.APPROVAL, a.project?.substringAfterLast('/').orEmpty(), a.title, task = a.task) } +
            r.attention.unblock.map { u -> Need(NeedKind.UNBLOCK, u.session, u.text) }
        val projects = r.projects.map { p ->
            Project(
                name = p.name, done = p.parts.count { it.state == "done" }, total = maxOf(p.partsTotal, p.parts.size),
                parts = p.parts.map { it.state }, waiting = p.waitingOn.map { w -> waiting(w) }, next = p.next,
            )
        }
        val day = Instant.ofEpochSecond(end).atZone(zone).toLocalDate()
        return Page(
            day = day, dayBefore = day.minusDays(1), start = start, end = end, windowS = (end - start).toInt(),
            fromLastMessage = r.window.startSource == "last_message", needs = needs, cards = cards, axis = axis, projects = projects,
            counts = Counts(
                done = cards.count { it.icon == Icon.OK }, running = cards.count { it.icon == Icon.RUNNING },
                stopped = cards.count { it.icon == Icon.STOPPED }, asking = cards.count { it.icon == Icon.QUESTION },
            ),
        )
    }

    /** «!: /clear», «domanda: …», «ok: …» del rapporto: il testo senza il prefisso. */
    private fun waiting(w: String): String = Regex("""^(?:!|domanda|ok)\s*:\s*""").replace(w, "")

    /**
     * Il titolo di un lavoro della coda (mai «…» nel corpo dei testi): il relay oggi taglia il prompt con «…». Se è lungo o
     * tagliato, si tiene la prima frase o il pezzo prima dei due punti; altrimenti fino all'ultima parola intera. La
     * correzione vera è un titolo breve dal relay (richiesta 4 della pagina Notte).
     */
    fun shortTitle(raw: String): String {
        val t = raw.trim()
        val cut = t.endsWith("…")
        if (!cut && t.length <= 60) return t.trimEnd('.')
        val body = t.removeSuffix("…")
        Regex("""^(.{12,}?)(?:[.;:]\s|\.$)""").find(body)?.let { return it.groupValues[1].trim() }
        if (!cut) return body.trimEnd('.')
        return body.substringBeforeLast(' ').trimEnd(',', ';', ':', ' ')
    }
}
