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
}
