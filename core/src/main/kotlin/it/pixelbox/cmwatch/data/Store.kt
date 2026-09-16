package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.State

/** Persistenza dell'ultimo stato, degli eventi (30 giorni) e della coda dei comandi. Room dietro; MemoryStore nei test. */
interface Store {
    /** Stato e istante di ricezione (epoch s). */
    suspend fun loadState(): Pair<State, Long>?
    suspend fun saveState(s: State, receivedAt: Long)
    suspend fun loadEvents(): List<Event>
    suspend fun saveEvents(ev: List<Event>)
    suspend fun pruneEvents(olderThan: Long)
    suspend fun loadPending(): List<Cmd>
    suspend fun savePending(c: List<Cmd>)
    /** «Ritmo 5 ore» (Franz, 16/09 13:00): un campione della quota delle 5 ore per account, registrato dall'orologio. */
    suspend fun saveQuotaSample(account: String, sample: it.pixelbox.cmwatch.rules.QuotaHistory.Sample)
    /** I campioni dal più vecchio, per account, a partire da `since`. */
    suspend fun loadQuotaSamples(since: Long): Map<String, List<it.pixelbox.cmwatch.rules.QuotaHistory.Sample>>
    suspend fun pruneQuotaSamples(olderThan: Long)
}
