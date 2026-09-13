package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Contenuto della schermata in stile «brief mattutino» di Wear OS (Franz, 13/09 16:20): una card per dato, con
 * etichetta, numero grande, eventuale unità, riga secondaria e pillolina. Qui si decide cosa si vede e con che
 * testo; il disegno sta in `BriefCard`. Le card a zero non si mostrano: una card vuota occupa spazio e non dice nulla.
 */
object BriefCards {
    /** Tono della pillolina e del gauge: neutro, buono, da guardare, allarme, dato vecchio. */
    enum class Tone { NEUTRAL, GOOD, WARN, ALERT, STALE }

    /** Simbolo dentro il gauge, come le icone del brief. Il disegno lo scegli in `BriefCard`, qui sta il significato. */
    enum class Glyph { TIME, SESSIONS, QUESTION, NIGHT, SYNC }

    data class Card(
        val key: String,
        val label: String,
        val value: String,
        val unit: String? = null,
        val secondary: String? = null,
        val pill: String? = null,
        val tone: Tone = Tone.NEUTRAL,
        val progress: Float? = null,
        val glyph: Glyph = Glyph.TIME,
    )

    data class Labels(
        val quota: String, val week: String, val resetAt: String, val stale: String, val none: String,
        val active: String, val waitingPill: String, val noQuestions: String,
        val questions: String, val oldest: String,
        val night: String, val running: String, val nothingRunning: String,
        val update: String, val minutes: String, val now: String, val stopped: String,
    )

    private val RESET = DateTimeFormatter.ofPattern("EEE HH:mm")

    /** `personale` per primo, poi gli altri account in ordine: è l'account di Franz e lo guarda per primo. */
    fun quota(state: State?, l: Labels, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.ITALIAN): List<Card> {
        val quota = state?.quota ?: return emptyList()
        val order = quota.keys.sortedWith(compareBy({ if (it.lowercase() == "personale") 0 else 1 }, { it.lowercase() }))
        return order.map { account ->
            val q = quota.getValue(account)
            Card(
                key = "quota-$account",
                // L'etichetta è il solo nome dell'account: «Quota» lo dice già l'intestazione della sezione, e con
                // l'anello a destra la colonna è larga una quindicina di caratteri (misurato al polso, 13/09 16:41).
                label = account,
                value = q.h5?.toString() ?: l.none,
                unit = if (q.h5 != null) "%" else null,
                secondary = q.resetW7?.let { l.resetAt.format(RESET.withLocale(locale).format(Instant.ofEpochSecond(it).atZone(zone))) },
                pill = if (q.stale) l.stale else l.week.format(q.w7?.let { "$it %" } ?: l.none),
                // Scala di allarme sulla finestra di 5 ore: dal 90 % ambra, esaurita rosso e il gauge pulsa,
                // perché da lì non si lavora più (review UX, 13/09).
                tone = when {
                    q.stale -> Tone.STALE
                    (q.h5 ?: 0) >= 100 -> Tone.ALERT
                    (q.h5 ?: 0) >= 90 -> Tone.WARN
                    else -> Tone.NEUTRAL
                },
                progress = QuotaText.fraction(q.h5),
                glyph = Glyph.TIME,
            )
        }
    }

    /** Il lavoro: quante sessioni lavorano, quante domande aspettano, la coda della notte, quanto è fresco il PC. */
    fun work(state: State?, freshness: Freshness, now: Long, l: Labels): List<Card> {
        if (state == null) return emptyList()
        val live = state.sessions.filter { it.state != SessionState.GONE }
        val active = live.count { it.state == SessionState.WAITING || it.state == SessionState.BUSY || it.state == SessionState.AWAITING }
        val questions = state.sessions.mapNotNull { it.question }
        val cards = mutableListOf<Card>()
        cards += Card(
            key = "active",
            label = l.active,
            value = active.toString(),
            secondary = null,
            pill = if (questions.isEmpty()) l.noQuestions else l.waitingPill.format(questions.size),
            tone = if (questions.isEmpty()) Tone.GOOD else Tone.WARN,
            progress = if (live.isEmpty()) 0f else active.toFloat() / live.size,
            glyph = Glyph.SESSIONS,
        )
        questions.minByOrNull { it.askedAt }?.let { oldest ->
            cards += Card(
                key = "questions", label = l.questions, value = questions.size.toString(),
                pill = l.oldest.format(Durations.since(oldest.askedAt, now)), tone = Tone.WARN,
                // Il gauge dice quante delle sessioni vive stanno aspettando te.
                progress = if (live.isEmpty()) 1f else questions.size.toFloat() / live.size,
                glyph = Glyph.QUESTION,
            )
        }
        val night = state.night
        if (night.queued > 0 || night.running != null) {
            cards += Card(
                key = "night", label = l.night, value = night.queued.toString(),
                pill = night.running?.let { l.running.format(it) } ?: l.nothingRunning,
                tone = if (night.running != null) Tone.NEUTRAL else Tone.GOOD,
                // Quanta coda è già passata: una in corso su quelle che restano.
                progress = if (night.running == null) 0f else 1f / (night.queued + 1),
                glyph = Glyph.NIGHT,
            )
        }
        val age = ((now - state.ts) / 60).toInt()
        cards += Card(
            key = "update",
            label = l.update,
            value = if (age <= 0) l.now else age.toString(),
            unit = if (age <= 0) null else l.minutes,
            secondary = if (freshness is Freshness.Stale) l.stopped else null,
            pill = state.host.takeIf { it.isNotBlank() },
            tone = if (freshness is Freshness.Fresh) Tone.GOOD else Tone.STALE,
            // Il gauge si riempie mentre il dato invecchia: pieno = PC fermo, e allora pulsa.
            progress = (((now - state.ts).toFloat() / Freshness.STALE_AFTER_S)).coerceIn(0f, 1f),
            glyph = Glyph.SYNC,
        )
        return cards
    }
}
