package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.State
import java.time.ZoneId

/**
 * La Panoramica del telefono (restyling 30/09): il brief dell'orologio con le sue regole. Anelli per account, «Adesso»,
 * domande, contesto, «Oggi», notte e ora dell'aggiornamento.
 */
object PhoneOverview {
    /**
     * `pace` null senza almeno due campioni, senza l'ora della ripartenza o con un dato vecchio (si proietterebbe dal
     * passato): l'anello resta, il ritmo no. `weekResetAt`: quando si azzera la settimana, per la pillola come sul polso,
     * finché non è passata anche con un dato vecchio (Franz, 02/10 16:51); `stale`: la lettura del conto è vecchia.
     */
    data class Ring(
        val account: String, val personal: Boolean, val h5: Int?, val w7: Int?, val resetAt: Long?, val pace: QuotaHistory.Pace?,
        val weekResetAt: Long? = null, val stale: Boolean = false,
    )

    /**
     * La riga della quota nella home: con almeno un account aggiornato quelli non aggiornati non si mostrano, e l'altro
     * prende tutta la larghezza (segnalazione 05/10 20:54); se sono tutti vecchi restano, con «non aggiornata».
     */
    fun lineRings(rings: List<Ring>): List<Ring> = rings.filter { !it.stale }.ifEmpty { rings }

    /** Una riga del contesto: le soglie della card delle misure, più modello ed effort come li legge il PC. */
    data class ContextRow(val name: String, val pct: Int, val tone: BriefCards.Tone, val model: String?, val effort: String?)

    data class Model(
        val rings: List<Ring>, val now: WorkPanel.Now, val questions: WorkPanel.Questions?,
        val contexts: List<ContextRow>, val today: List<DayBars.Bar>, val nightQueued: Int, val updated: WorkPanel.Updated,
    )

    /** Ordine e regola del reset di `PhoneBoard.quotaRows`: personale prima, nessuna ora da un dato vecchio o passato. */
    fun build(
        state: State, events: List<Event>, samples: Map<String, List<QuotaHistory.Sample>>, now: Long, zone: ZoneId, stale: Boolean,
    ): Model {
        val rings = PhoneBoard.quotaRows(state, now, dataStale = stale).map { row ->
            val q = state.quota.getValue(row.account)
            val pace = samples[row.account]?.takeIf { it.size >= 2 && row.resetAt != null && !stale && !row.stale }
                ?.let { QuotaHistory.pace(it, row.resetAt!!, now) }
            val weekReset = q.resetW7?.takeIf { it > now }
            Ring(row.account, row.personal, q.h5, q.w7, row.resetAt, pace, weekReset, row.stale)
        }
        val contexts = WorkPanel.contexts(state).map { c ->
            val s = state.sessions.first { it.name == c.name }
            ContextRow(c.name, c.pct, c.tone, ModelText.short(s.model), s.effort?.trim()?.takeIf { it.isNotEmpty() })
        }
        val updated = WorkPanel.Updated(((now - state.ts) / 60).toInt().coerceAtLeast(0), state.host, stale)
        return Model(
            rings, WorkPanel.now(state), WorkPanel.questions(state, now), contexts,
            DayBars.today(events, now, zone), state.night.queued, updated,
        )
    }
}
