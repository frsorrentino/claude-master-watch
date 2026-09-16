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

    /** Il segno dell'account davanti al titolo, come nella tile: cerchio personale, quadrato lavoro (Franz, 16/09 14:41). */
    enum class Shape { CIRCLE, SQUARE }

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
        /**
         * Secondo valore per l'anello concentrico (proposta 46, 16/09): nella quota, fuori le 5 ore e dentro la
         * settimana. Assente quando il numero grande è già la settimana: un anello dentro l'altro con lo stesso dato
         * non direbbe niente.
         */
        val progress2: Float? = null,
        val glyph: Glyph = Glyph.TIME,
        /** Tono dell'anello interno e della sua pillola: la settimana ha soglie sue (Franz, 16/09 11:52). */
        val tone2: Tone = Tone.NEUTRAL,
        /** Solo nelle card della quota: di quale account è, detto con la forma invece che con il nome. */
        val shape: Shape? = null,
    )

    data class Labels(
        val quota: String, val week: String, val resetAt: String, val stale: String, val none: String,
        val active: String, val waitingPill: String, val noQuestions: String,
        val questions: String, val oldest: String,
        val night: String, val running: String, val nothingRunning: String,
        val update: String, val minutes: String, val now: String, val stopped: String,
        /** «settimana»: la pillolina quando il numero grande è già la settimana, senza lettura delle 5 ore. */
        val weekOnly: String = "",
        /** Il titolo della card della quota, uguale in Panoramica e nella Scheda (Franz, 16/09 14:41). */
        val quotaTitle: String = "Quota",
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
            val weekDay = q.resetW7?.let { RESET.withLocale(locale).format(Instant.ofEpochSecond(it).atZone(zone)) }
            val weekReset = weekDay?.let { l.resetAt.format(it) }
            Card(
                key = "quota-$account",
                // Il titolo è il tema, «Quota», uguale in Panoramica e nella Scheda; l'account lo dice la forma (Franz, 16/09
                // 14:41). Prima era il nome dell'account qui e «Quota» nella Scheda: la stessa card con due titoli.
                label = l.quotaTitle,
                shape = if (Accounts.isPersonalQuota(account, q)) Shape.CIRCLE else Shape.SQUARE,
                value = big?.toString() ?: l.none,
                unit = if (big != null) "%" else null,
                // Sotto la percentuale delle 5 ore la sua ripartenza (contratto 1.3); la settimanale sta con la settimana,
                // altrimenti «gio 04:00» sotto il 7 % sembrava il reset delle 5 ore (Franz, 14/09 10:38).
                // Con la sola settimana il reset va nella pillola come negli altri casi: accanto all'anello «reset gio 04:00»
                // veniva tagliato (Franz, 16/09 14:37).
                secondary = if (soloSettimana) null else q.resetH5?.let { l.resetAt.format(HHMM.withLocale(locale).format(Instant.ofEpochSecond(it).atZone(zone))) },
                pill = when {
                    q.stale -> l.stale
                    soloSettimana -> l.weekOnly + (weekDay?.let { " · $it" } ?: "")
                    // La ripartenza settimanale dentro la pillola: ora la pillola sta a tutta larghezza sotto l'anello
                    // e non va più a capo, e la riga sua in fondo rendeva questa card diversa da quella della Scheda
                    // (Franz, 16/09 13:09). Se il numero grande è la settimana, il reset sta già sotto di lui.
                    else -> l.week.format(q.w7?.let { "$it %" } ?: l.none) + (weekDay?.let { " · $it" } ?: "")
                },
                note = null,
                // Scala di allarme sulla finestra di 5 ore: dal 90 % ambra, esaurita rosso e il gauge pulsa,
                // perché da lì non si lavora più (review UX, 13/09). Per la settimana, l'ambra dall'80 %, la soglia di stop.
                tone = when {
                    q.stale -> Tone.STALE
                    (big ?: 0) >= 100 -> Tone.ALERT
                    soloSettimana && (big ?: 0) >= 80 -> Tone.WARN
                    (q.h5 ?: 0) >= 90 -> Tone.WARN
                    else -> Tone.NEUTRAL
                },
                // Sempre due anelli (Franz, 16/09 14:41): fuori le 5 ore, vuoto quando non c'è la lettura; dentro la settimana.
                progress = if (soloSettimana) 0f else QuotaText.fraction(big),
                progress2 = q.w7?.let { QuotaText.fraction(it) },
                glyph = Glyph.TIME,
                // La settimana col suo tono: ambra dall'80 %, la soglia di stop; rosso esaurita. Con la settimana all'82 %
                // l'anello interno restava azzurro come le 5 ore e non avvisava (Franz, 16/09 11:52).
                tone2 = when {
                    q.stale -> Tone.STALE
                    (q.w7 ?: 0) >= 100 -> Tone.ALERT
                    (q.w7 ?: 0) >= 80 -> Tone.WARN
                    else -> Tone.NEUTRAL
                },
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
