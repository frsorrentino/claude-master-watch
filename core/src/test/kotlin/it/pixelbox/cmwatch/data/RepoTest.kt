package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.transport.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RepoTest {
    private class Slow(var delayMs: Long, base: FakeTransport) : Transport by base {
        var fail = false
        override suspend fun send(cmd: Cmd): CmdResult {
            delay(delayMs)
            if (fail) throw TransportException.Network("down")
            return CmdResult(cmd.id, true, "answered ${cmd.arg}. ok", 0)
        }
    }
    /** Uno scope sullo scheduler del test ma fuori dal TestScope: i suoi job girano con advanceUntilIdle e non tengono vivo il test. */
    private fun TestScope.bg() = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
    private fun TestScope.idle() = testScheduler.advanceUntilIdle()
    private var clock = 1789210800L
    private var online = true
    private fun fake() = FakeTransport({ Fixtures.read("$it.json") }, { clock })

    @Test fun opensFromStoreThenFollowsTransport() = runTest {
        val store = MemoryStore()
        store.saveState(ContractJson.decodeState(Fixtures.stateIdle), clock - 10)
        val repo = Repo(store, fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.loadFromStore()
        assertEquals(1, repo.snapshot.value.state!!.sessions.size)     // da Room, subito
        repo.start(); idle()
        assertEquals(4, repo.snapshot.value.state!!.sessions.size)     // poi dal transport
        assertEquals(Freshness.Fresh, repo.snapshot.value.freshness)
        assertEquals(4, store.loadState()!!.first.sessions.size)
        assertEquals(6, repo.events.value.size)
    }

    @Test fun staleStateIsFlagged() = runTest {
        val tr = fake(); tr.useFixture("state-3-stale")
        val repo = Repo(MemoryStore(), tr, bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        assertTrue(repo.snapshot.value.freshness is Freshness.Stale)
    }

    @Test fun answerIsOptimisticThenConfirmed() = runTest {
        val repo = Repo(MemoryStore(), fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        val got = mutableListOf<CmdResult>(); bg().launch { repo.results.collect { got += it } }; idle()
        val id = repo.answer("ledger-api", 1); idle()
        assertTrue(repo.snapshot.value.pending.isEmpty())
        assertEquals(1, got.size); assertEquals(id, got[0].id); assertTrue(got[0].ok)
        assertNull(repo.snapshot.value.state!!.sessions.first { it.name == "ledger-api" }.question)
        assertEquals(got[0], repo.resultsById.value[id])            // leggibile anche da chi si iscrive dopo
    }

    @Test fun noResultWithin20sBecomesFailedAndRetryIsSafe() = runTest {
        val slow = Slow(25_000, fake())
        val repo = Repo(MemoryStore(), slow, bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        val id = repo.answer("ledger-api", 1)
        advanceTimeBy(21_000)
        assertEquals(PendingStatus.FAILED, repo.snapshot.value.pending.single().status)
        slow.delayMs = 10; repo.retry(id); idle()
        assertTrue(repo.snapshot.value.pending.isEmpty())
    }

    @Test fun networkErrorBecomesFailed() = runTest {
        val slow = Slow(10, fake()).apply { fail = true }
        val repo = Repo(MemoryStore(), slow, bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        repo.prompt("atlas-shop", "x"); idle()
        assertEquals(PendingStatus.FAILED, repo.snapshot.value.pending.single().status)
    }

    @Test fun offlineQueuesAtMostTenAndDropsAfterTenMinutes() = runTest {
        online = false
        val store = MemoryStore()
        val repo = Repo(store, fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        val notices = mutableListOf<Repo.Notice>(); bg().launch { repo.notices.collect { notices += it } }; idle()
        repeat(12) { repo.prompt("atlas-shop", "m$it") }
        idle()
        assertEquals(10, repo.snapshot.value.pending.size)
        assertTrue(repo.snapshot.value.pending.all { it.status == PendingStatus.QUEUED })
        assertEquals(10, store.loadPending().size)
        assertEquals(2, notices.count { it is Repo.Notice.QueueFull })
        clock += 11 * 60; online = true; repo.flushQueue(); idle()
        assertTrue(repo.snapshot.value.pending.isEmpty())                 // scartati con avviso, non inviati
        assertEquals(Repo.Notice.Dropped(10), notices.last())
        online = false; repo.prompt("atlas-shop", "fresh"); online = true; repo.flushQueue(); idle()
        assertTrue(repo.snapshot.value.pending.isEmpty())                 // inviato
        assertTrue(store.loadPending().isEmpty())
    }
}

class MemoryStore : Store {
    private var state: Pair<State, Long>? = null
    private var events: List<Event> = emptyList()
    private var pending: List<Cmd> = emptyList()
    override suspend fun loadState() = state
    override suspend fun saveState(s: State, receivedAt: Long) { state = s to receivedAt }
    override suspend fun loadEvents() = events
    override suspend fun saveEvents(ev: List<Event>) { events = (ev + events).distinctBy { it.key }.sortedByDescending { it.ts } }
    override suspend fun pruneEvents(olderThan: Long) { events = events.filter { it.ts >= olderThan } }
    override suspend fun loadPending() = pending
    override suspend fun savePending(c: List<Cmd>) { pending = c }
}
