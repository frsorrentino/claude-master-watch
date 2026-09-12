package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Testi delle notifiche locali (design, sezione 2). Le etichette arrivano da strings.xml. */
object NotificationTexts {
    const val CHANNEL_QUESTIONS = "questions"
    const val CHANNEL_OUTCOMES = "outcomes"
    const val CHANNEL_GONE = "gone"
    const val CHANNEL_QUOTA = "quota"

    data class Labels(val open: String, val reply: String, val resetWeek: String, val window5h: String)
    data class Note(val title: String, val body: String, val actions: List<String>, val channel: String)

    /** «❓ nome» + testo intero; azioni: le prime due opzioni + Apri. */
    fun question(s: Session, l: Labels): Note {
        val q = s.question ?: return Note("❓ ${s.name}", "", listOf(l.open), CHANNEL_QUESTIONS)
        val actions = q.options.take(2).map { QuestionRules.optionLabel(it) } + l.open
        return Note("❓ ${s.name}", q.text, actions, CHANNEL_QUESTIONS)
    }

    fun outcome(s: Session, l: Labels): Note = Note("✓ ${s.name}", s.outcome?.short ?: "", listOf(l.open), CHANNEL_OUTCOMES)

    fun gone(name: String, l: Labels): Note = Note("✗ $name", "", listOf(l.open), CHANNEL_GONE)

    fun quota(account: String, q: QuotaAccount, l: Labels, zone: ZoneId, locale: Locale = Locale.ITALIAN, threshold: Int = 95): Note {
        val h5 = q.h5 ?: 0; val w7 = q.w7 ?: 0
        val weekly = w7 >= threshold && w7 >= h5
        val pct = if (weekly) w7 else h5
        val body = if (weekly && q.resetW7 != null) {
            "${l.resetWeek} " + DateTimeFormatter.ofPattern("EEE HH:mm", locale).format(Instant.ofEpochSecond(q.resetW7).atZone(zone))
        } else l.window5h
        return Note("⚠ $pct % $account", body, listOf(l.open), CHANNEL_QUOTA)
    }
}
