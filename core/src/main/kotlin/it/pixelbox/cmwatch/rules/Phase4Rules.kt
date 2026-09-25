package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.Night
import it.pixelbox.cmwatch.contract.Project
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Recap
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Tasto ▶ della lettura vocale: sopra `tts.min_chars` o per esito/risposta/domanda (design, «Decisioni fisse»). */
object SpeakRules {
    enum class Kind { PLAIN, OUTCOME, ANSWER, QUESTION }
    fun showButton(text: String, kind: Kind, minChars: Int): Boolean =
        text.isNotBlank() && (kind != Kind.PLAIN || text.length > minChars)
}

/** Timeline: `/events` per giorno, più recenti prima; riga «HH:mm · titolo · sessione · corpo». */
object TimelineText {
    data class Group(val day: String, val rows: List<String>)

    fun row(e: Event, zone: ZoneId): String {
        val time = DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochSecond(e.ts).atZone(zone))
        val parts = mutableListOf(time, e.title)
        if (e.session != null && !e.title.contains(e.session)) parts += e.session
        if (e.body.isNotBlank()) parts += e.body
        return parts.joinToString(" · ")
    }

    fun groups(events: List<Event>, zone: ZoneId, session: String? = null, locale: Locale = Locale.ITALIAN): List<Group> {
        val dayFmt = DateTimeFormatter.ofPattern("d MMM", locale)
        return events.filter { session == null || it.session == session }
            .sortedByDescending { it.ts }
            .groupBy { Instant.ofEpochSecond(it.ts).atZone(zone).toLocalDate() }
            .entries.sortedByDescending { it.key }
            .map { (day, list) -> Group(dayFmt.format(day), list.map { row(it, zone) }) }
    }
}

/** Lancia: solo percorsi pubblicati dal PC in `projects`, mai percorsi liberi dal polso (design, sezione 5). */
object LaunchRules {
    fun allowed(path: String, projects: List<Project>): Boolean = path.isNotBlank() && projects.any { it.path == path }

    /** «Nuova sessione» (contratto 1.13): dal progetto usato più di recente; chi non ha mai avuto sessioni in fondo, per nome. */
    fun ordered(projects: List<Project>): List<Project> =
        projects.sortedWith(compareBy<Project> { it.lastUsed == null }.thenByDescending { it.lastUsed ?: 0L }.thenBy { it.name.lowercase() })

    /**
     * Percorso da lanciare per far ripartire una sessione chiusa: `resume` non vale per una sessione che non esiste
     * più, il comando giusto è `launch` sul suo progetto (Franz, 13/09 18:11). Si accoppia per coda del percorso,
     * perché la sessione porta il percorso relativo e il progetto quello assoluto; in mancanza, per nome.
     */
    fun pathFor(s: Session, projects: List<Project>): String? =
        projects.firstOrNull { it.path.endsWith("/" + s.project) || it.path == s.project }?.path
            ?: projects.firstOrNull { it.name == s.name }?.path
}

/** Segui: l'ongoing activity «▶ nome 4 m» esiste solo mentre la sessione seguita lavora. */
object FollowRules {
    fun ongoing(state: State): Session? =
        state.sessions.firstOrNull { it.followed && (it.state == SessionState.BUSY || it.state == SessionState.AWAITING) }
    fun status(s: Session, now: Long): String = "▶ ${SessionsText.row(s, now)}"
}

/** Quota: due anelli e il reset (design, sezione 2). */
object QuotaText {
    data class Labels(val week: String, val reset: String, val stale: String, val none: String)

    fun fraction(pct: Int?): Float = ((pct ?: 0).coerceIn(0, 100)) / 100f

    fun h5Line(account: String, q: QuotaAccount, l: Labels): String {
        val parts = mutableListOf(account, q.h5?.let { "$it %" } ?: l.none)
        if (q.stale) parts += l.stale
        return parts.joinToString(" · ")
    }

    fun w7Line(q: QuotaAccount, l: Labels, zone: ZoneId, locale: Locale = Locale.ITALIAN): String {
        val parts = mutableListOf("${l.week} ${q.w7?.let { "$it %" } ?: l.none}")
        q.resetW7?.let { parts += "${l.reset} " + DateTimeFormatter.ofPattern("EEE HH:mm", locale).format(Instant.ofEpochSecond(it).atZone(zone)) }
        return parts.joinToString(" · ")
    }
}

/** Recap del giorno e coda della notte. */
object RecapText {
    data class Row(val done: String, val next: String?)
    fun rows(recap: Recap): List<Row> = recap.items.map { Row("${it.project} · ${it.done}", it.next?.let { n -> "→ $n" }) }
    fun night(n: Night, none: String): String = "${n.queued} · ${n.running ?: none}"
}
