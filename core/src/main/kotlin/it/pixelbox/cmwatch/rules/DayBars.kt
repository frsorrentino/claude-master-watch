package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import java.time.Instant
import java.time.ZoneId

/**
 * «Oggi» (proposta 34, fase 1): una barra per ora con quanto hanno lavorato le sessioni, contata dagli eventi che
 * l'orologio ha già in casa (7 giorni). Si leggono come le barre dei passi: si vede subito quando è stata la giornata.
 * Le barre arrivano fino all'ora corrente, mai oltre: ore future vuote direbbero «non hai fatto niente» invece di
 * «non è ancora successo».
 */
object DayBars {
    data class Bar(val hour: Int, val count: Int)

    /** Gli eventi che dicono lavoro. La quota cambia da sola e non è lavoro di nessuno: fuori. */
    private val WORK = setOf(EventKind.LAUNCHED, EventKind.ANSWERED, EventKind.OUTCOME, EventKind.QUESTION, EventKind.RESUMED)

    fun today(events: List<Event>, now: Long, zone: ZoneId): List<Bar> {
        val inizio = Instant.ofEpochSecond(now).atZone(zone).toLocalDate().atStartOfDay(zone).toEpochSecond()
        val oraCorrente = ((now - inizio) / 3600).toInt().coerceIn(0, 23)
        val conteggi = IntArray(24)
        for (e in events) {
            if (e.kind !in WORK || e.ts < inizio) continue
            val h = ((e.ts - inizio) / 3600).toInt()
            if (h in 0..oraCorrente) conteggi[h]++
        }
        return (0..oraCorrente).map { Bar(it, conteggi[it]) }
    }

    /** L'ora più piena: è la scala con cui si disegnano le barre. */
    fun peak(bars: List<Bar>): Int = bars.maxOfOrNull { it.count } ?: 0
}
