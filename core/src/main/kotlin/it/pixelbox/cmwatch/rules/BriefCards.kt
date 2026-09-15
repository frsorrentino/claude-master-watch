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
        /** Riga sotto la pillolina: la ripartenza settimanale nella card della quota. */
        val note: String? = null,
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
        /** «settimana»: la pillolina quando il numero grande è già la settimana, senza lettura delle 5 ore. */
        val weekOnly: String = "",
    )

    private val RESET = DateTimeFormatter.ofPattern("EEE HH:mm")
    private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

    /** `personale` per primo, poi gli altri account in ordine: è l'account di Franz e lo guarda per primo. */
    fun quota(state: State?, l: Labels, zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.ITALIAN): List<Card> {
        val quota = state?.quota ?: return emptyList()
        // Dal contratto 1.8 l'account personale lo dice il tipo, non il nome.
        val order = quota.keys.sortedWith(compareBy({ if (Accounts.isPersonalQuota(it, quota.getValue(it))) 0 else 1 }, { it.lowercase() }))
        return order.map { account ->
            val q = quota.getValue(account)
            // Senza lettura delle 5 ore (`h5: null`) il numero grande è la settimana, con il suo reset sotto: prima la
            // card mostrava solo «—» (Franz, 15/09 15:34).
            val soloSettimana = q.h5 == null && q.w7 != null
            val big = if (soloSettimana) q.w7 else q.h5
            val weekReset = q.resetW7?.let { l.resetAt.format(RESET.withLocale(locale).format(Instant.ofEpochSecond(it).atZone(zone))) }
            Card(
                key = "quota-$account",
                // L'etichetta è il solo nome dell'account: «Quota» lo dice già l'intestazione della sezione, e con
                // l'anello a destra la colonna è larga una quindicina di caratteri (misurato al polso, 13/09 16:41).
                label = account,
                value = big?.toString() ?: l.none,
                unit = if (big != null) "%" else null,
                // Sotto la percentuale delle 5 ore la sua ripartenza (contratto 1.3); la settimanale sta con la settimana,
                // altrimenti «gio 04:00» sotto il 7 % sembrava il reset delle 5 ore (Franz, 14/09 10:38).
                secondary = if (soloSettimana) weekReset else q.resetH5?.let { l.resetAt.format(HHMM.withLocale(locale).format(Instant.ofEpochSecond(it).atZone(zone))) },
                pill = when {
                    q.stale -> l.stale
                    soloSettimana -> l.weekOnly
                    else -> l.week.format(q.w7?.let { "$it %" } ?: l.none)
                },
                // La ripartenza settimanale su una riga sua sotto la pillolina: dentro andava a capo e il bordo tondo
                // la tagliava (visto al polso, 14/09 11:33). Se il numero grande è la settimana, sta già sotto di lui.
                note = if (soloSettimana) null else weekReset,
                // Scala di allarme sulla finestra di 5 ore: dal 90 % ambra, esaurita rosso e il gauge pulsa,
                // perché da lì non si lavora più (review UX, 13/09). Per la settimana, l'ambra dall'80 %, la soglia di stop.
                tone = when {
                    q.stale -> Tone.STALE
                    (big ?: 0) >= 100 -> Tone.ALERT
                    soloSettimana && (big ?: 0) >= 80 -> Tone.WARN
                    (q.h5 ?: 0) >= 90 -> Tone.WARN
                    else -> Tone.NEUTRAL
                },
                progress = QuotaText.fraction(big),
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
