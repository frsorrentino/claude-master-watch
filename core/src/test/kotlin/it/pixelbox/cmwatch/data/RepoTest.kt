package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.rules.QuotaHistory
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.transport.*
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
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
        override suspend fun send(cmd: Cmd, onWritten: () -> Unit): CmdResult {
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
        assertEquals(9 + 14, repo.events.value.size)                  // i 9 della fixture (1.18) e i 14 sparsi della demo (16/09)
    }

    /** Revisione 29/09: una sveglia FCM a freddo arriva prima del caricamento da Room; lo stato vecchio non deve coprire quello fresco. */
    @Test fun freshRefreshIsNotOverwrittenByTheStoreLoad() = runTest {
        val mem = MemoryStore()
        mem.saveState(ContractJson.decodeState(Fixtures.stateIdle), clock - 3600)
        // Room lento al primo avvio: il GET della sveglia arriva prima.
        val store = object : Store by mem { override suspend fun loadState() = mem.loadState().also { delay(1_000) } }
        val repo = Repo(store, fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(live = false)
        testScheduler.advanceTimeBy(10)
        repo.refresh()
        idle()
        assertEquals(4, repo.snapshot.value.state!!.sessions.size)
    }

    /** Contratto 1.19: l'immagine va in /share prima del comando, e il comando porta il suo id. */
    @Test fun reportUploadsTheImageThenSendsTheCommand() = runTest {
        val tr = fake()
        val repo = Repo(MemoryStore(), tr, bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle()
        val id = repo.report("atlas-shop", "grey button", "image/jpeg", byteArrayOf(1, 2, 3), maxBytes = 1_500_000); idle()
        val r = repo.resultsById.value.getValue(id)
        assertTrue(r.text, r.ok); assertTrue(r.text.startsWith("sent to atlas-shop: image saved as "))
    }

    @Test fun reportWithoutNetworkFailsInsteadOfQueueing() = runTest {
        val repo = Repo(MemoryStore(), fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle(); online = false
        try { repo.report("atlas-shop", "x", "image/jpeg", byteArrayOf(1), maxBytes = 1_500_000); fail("expected Network") }
        catch (e: TransportException.Network) { }
        online = true
    }

    /** Un transport il cui stato si cambia a mano, per mandare al Repo una sequenza di stati. */
    private class Pushing(base: FakeTransport, first: State) : Transport by base {
        val flow = MutableStateFlow(first)
        override val state: Flow<State> = flow
    }

    private fun conH5(ts: Long, h5: Int): State {
        val base = ContractJson.decodeState(Fixtures.stateQuestion)
        return base.copy(ts = ts, quota = base.quota.mapValues { (k, q) -> if (k == "personal") q.copy(h5 = h5) else q })
    }

    // «Ritmo 5 ore» (Franz, 16/09 13:00): l'orologio registra da sé un campione della quota a ogni stato che riceve.
    @Test fun ogniStatoRegistraUnCampioneDellaQuotaPerIlRitmo() = runTest {
        val tr = Pushing(fake(), conH5(clock, 10))
        val store = MemoryStore()
        val repo = Repo(store, tr, bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle()
        clock += 600; tr.flow.value = conH5(clock, 14); idle()
        // Stesso valore un minuto dopo: nessun campione nuovo, la linea non si riempie di punti uguali.
        clock += 60; tr.flow.value = conH5(clock, 14); idle()
        assertEquals(listOf(10, 14), repo.quotaSamples.value.getValue("personal").map { it.pct })
        // L'account di lavoro non ha la lettura delle 5 ore (`h5` null): niente campioni inventati.
        assertNull(repo.quotaSamples.value["work"])
        // Riaprendo l'app i campioni tornano da Room, prima che arrivi uno stato nuovo.
        val riaperto = Repo(store, fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        riaperto.loadFromStore()
        assertEquals(2, riaperto.quotaSamples.value.getValue("personal").size)
    }

    @Test fun iCampioniPiuVecchiDellaFinestraSiButtano() = runTest {
        val tr = Pushing(fake(), conH5(clock, 10))
        val repo = Repo(MemoryStore(), tr, bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle()
        clock += 7 * 3600; tr.flow.value = conH5(clock, 3); idle()
        assertEquals(listOf(3), repo.quotaSamples.value.getValue("personal").map { it.pct })
    }

    /** Conta chi sta ascoltando gli stream del transport: con l'app chiusa devono essere zero. */
    private class Counting(base: FakeTransport) : Transport by base {
        val listening = MutableStateFlow(0)
        override val state: Flow<State> = base.state.onStart { listening.value++ }.onCompletion { listening.value-- }
        override val events: Flow<List<Event>> = base.events.onStart { listening.value++ }.onCompletion { listening.value-- }
    }

    // Batteria (Franz, 14/09 17:18): gli stream aperti ad app chiusa costavano 40 mAh in 15 ore; chiusa, la sveglia è FCM.
    @Test fun conLAppChiusaGliStreamSiChiudonoEAllAperturaSiRiaprono() = runTest {
        val tr = Counting(fake())
        val repo = Repo(MemoryStore(), tr, bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle()
        assertEquals(2, tr.listening.value)
        repo.live(false); idle()
        assertEquals(0, tr.listening.value)
        repo.live(true); idle()
        assertEquals(2, tr.listening.value)
    }

    @Test fun partendoInBackgroundNessunoStreamMaIlGetDellaSveglia() = runTest {
        val tr = Counting(fake())
        val repo = Repo(MemoryStore(), tr, bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(live = false); idle()
        assertEquals(0, tr.listening.value)
        assertTrue(repo.refresh())
        assertEquals(4, repo.snapshot.value.state!!.sessions.size)
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

    // Il Terminale dal vivo chiede `screen` e `last` ogni pochi secondi: i loro risultati non sono azioni dell'utente e
    // non devono vibrare né mostrare la conferma (15/09 23:45). Restano leggibili per id.
    @Test fun leCattureDelTerminaleNonSonoAzioniDellUtente() = runTest {
        val repo = Repo(MemoryStore(), fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        val got = mutableListOf<String>(); bg().launch { repo.userResults.collect { got += it.id } }; idle()
        val screen = repo.command(CmdOp.SCREEN, "atlas-shop", null)
        val last = repo.command(CmdOp.LAST, "atlas-shop", null)
        val answer = repo.answer("ledger-api", 1); idle()
        assertEquals(listOf(answer), got)
        assertTrue(repo.resultsById.value.containsKey(screen))
        assertTrue(repo.resultsById.value.containsKey(last))
    }

    // Contratto 1.22: la chat chiede `transcript` ogni pochi secondi; i suoi risultati, anche i rifiuti, non sono avvisi.
    @Test fun leLettureDellaChatNonSonoAzioniDellUtente() = runTest {
        val repo = Repo(MemoryStore(), fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        val got = mutableListOf<String>(); bg().launch { repo.userResults.collect { got += it.id } }; idle()
        val page = repo.command(CmdOp.TRANSCRIPT, "atlas-shop", "50"); idle()
        assertTrue(got.isEmpty())
        assertTrue(repo.resultsById.value.containsKey(page))
    }

    // Segui e non seguire si vedono subito (Franz, 16/09 01:58): la campanella e il bordo non aspettano il PC, altrimenti
    // dopo la pressione lunga non cambia niente sullo schermo. Se il comando fallisce, il prossimo stato rimette a posto.
    @Test fun seguireSiVedeSubito() = runTest {
        val repo = Repo(MemoryStore(), fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        val nome = repo.snapshot.value.state!!.sessions.first { !it.followed }.name
        repo.command(CmdOp.FOLLOW, nome, null)
        assertTrue(repo.snapshot.value.state!!.sessions.first { it.name == nome }.followed)
        repo.command(CmdOp.UNFOLLOW, nome, null)
        assertFalse(repo.snapshot.value.state!!.sessions.first { it.name == nome }.followed)
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
        // Dalla revisione del 30/09 l'undicesimo e il dodicesimo sono rifiutati a voce alta, non scartati in silenzio.
        repeat(12) { runCatching { repo.prompt("atlas-shop", "m$it") } }
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

    /**
     * Ogni accensione della demo semina una rampa sulle ultime tre ore. Aggiunte una all'altra, rampe di orari diversi si
     * intrecciano e il grafico del ritmo diventava un dente di sega (ripresa della Panoramica, 21/09): la demo sostituisce.
     */
    @Test fun seedingTheDemoTwiceReplacesTheRampInsteadOfStackingIt() = runTest {
        val store = MemoryStore()
        val repo = Repo(store, fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        val prima = (0..9).map { k -> QuotaHistory.Sample(clock - 10_000 + k * 1000L, 2 + k * 2) }
        val dopo = (0..9).map { k -> QuotaHistory.Sample(clock - 9_500 + k * 1000L, 3 + k * 2) }
        repo.seedQuotaSamples(mapOf("personal" to prima))
        repo.seedQuotaSamples(mapOf("personal" to dopo))
        assertEquals(dopo, repo.quotaSamples.value["personal"])
    }

    /** Contratto 1.22: ogni comando dice da che dispositivo arriva, così il relay sceglie il prefisso giusto. */
    @Test fun commandsCarryTheDevice() = runTest {
        val tr = Recording(fake())
        val repo = Repo(MemoryStore(), tr, bg(), { clock }, { online }, "test", freshnessTickMs = 0, device = "phone")
        repo.start(); idle()
        val id = repo.prompt("atlas-shop", "run the tests"); idle()
        assertEquals("phone", tr.cmds.single { it.id == id }.device)
    }

    /** Lo stato preciso (30/09 22:13): appena il comando è scritto sul canale è «inviato al PC», prima del risultato. */
    @Test fun pendingBecomesSentOnceWritten() = runTest {
        val tr = Paused(fake())
        val repo = Repo(MemoryStore(), tr, bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle()
        val id = repo.prompt("atlas-shop", "run the tests")
        tr.written.await()
        assertEquals(PendingStatus.SENT, repo.snapshot.value.pending.single { it.cmd.id == id }.status)
        tr.release.complete(Unit); idle()
        assertTrue(repo.snapshot.value.pending.none { it.cmd.id == id })
    }

    /** Un'immagine che il canale rifiuta si dice subito, con il motivo, sotto lo stesso id del messaggio. */
    @Test fun uploadFailureIsRecordedUnderTheMessageId() = runTest {
        val repo = Repo(MemoryStore(), fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle()
        try { repo.report("atlas-shop", "x", "image/jpeg", ByteArray(10), maxBytes = 5, id = "m1"); fail("expected TooLarge") }
        catch (e: TransportException.TooLarge) { }
        assertTrue(repo.uploads.value["m1"] is it.pixelbox.cmwatch.rules.ChatRules.Upload.Failed)
    }

    /** Revisione 30/09: senza rete le letture della chat non riempiono la coda dei comandi dell'utente. */
    @Test fun passiveReadsAreNeverQueued() = runTest {
        val repo = Repo(MemoryStore(), fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle(); online = false
        try { repo.command(CmdOp.TRANSCRIPT, "atlas-shop", "50"); fail("expected Network") } catch (e: TransportException.Network) { }
        assertTrue(repo.snapshot.value.pending.isEmpty())
        online = true
    }

    /** Con la coda piena un messaggio non si finge partito: il chiamante riceve l'errore. */
    @Test fun aFullQueueRefusesLoudly() = runTest {
        val repo = Repo(MemoryStore(), fake(), bg(), { clock }, { online }, "test", freshnessTickMs = 0)
        repo.start(); idle(); online = false
        repeat(10) { repo.prompt("atlas-shop", "m$it") }
        try { repo.prompt("atlas-shop", "one too many"); fail("expected QueueFull") } catch (e: Repo.QueueFull) { }
        online = true
    }

    /** Il trasporto della demo che si ferma dopo aver scritto il comando, finché il test non lo lascia andare. */
    private class Paused(private val inner: it.pixelbox.cmwatch.transport.Transport) : it.pixelbox.cmwatch.transport.Transport by inner {
        val written = kotlinx.coroutines.CompletableDeferred<Unit>()
        val release = kotlinx.coroutines.CompletableDeferred<Unit>()
        override suspend fun send(cmd: it.pixelbox.cmwatch.contract.Cmd, onWritten: () -> Unit): it.pixelbox.cmwatch.contract.CmdResult {
            onWritten(); written.complete(Unit); release.await(); return inner.send(cmd)
        }
    }

    /** Il trasporto della demo, con la lista dei comandi mandati: solo per guardarli nel test. */
    private class Recording(private val inner: it.pixelbox.cmwatch.transport.Transport) : it.pixelbox.cmwatch.transport.Transport by inner {
        val cmds = mutableListOf<it.pixelbox.cmwatch.contract.Cmd>()
        override suspend fun send(cmd: it.pixelbox.cmwatch.contract.Cmd, onWritten: () -> Unit) = inner.send(cmd, onWritten).also { cmds += cmd }
    }

    /** Revisione finale 01/10 (C1): l'invio programmato aspetta l'esito; senza rete o senza risposta non risulta partito. */
    @Test fun deliverWaitsForTheResult() = runTest {
        val slow = Slow(10, fake())
        val repo = Repo(MemoryStore(), slow, bg(), { clock }, { online }, "test", freshnessTickMs = 0); repo.start(); idle()
        val done = async(bg().coroutineContext) { repo.deliver(CmdOp.PROMPT, "atlas-shop", "x", "s1") }
        idle()
        assertTrue((done.await() as Repo.Delivery.Done).result.ok)

        slow.delayMs = 25_000
        val late = async(bg().coroutineContext) { repo.deliver(CmdOp.PROMPT, "atlas-shop", "x", "s2") }
        advanceTimeBy(21_000); idle()
        assertEquals(Repo.Delivery.NotSent, late.await())
        assertTrue(repo.snapshot.value.pending.none { it.cmd.id == "s2" })

        online = false
        assertEquals(Repo.Delivery.NotSent, repo.deliver(CmdOp.PROMPT, "atlas-shop", "x", "s3"))
        assertTrue(repo.snapshot.value.pending.isEmpty())
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
    private val samples = ArrayList<Pair<String, it.pixelbox.cmwatch.rules.QuotaHistory.Sample>>()
    override suspend fun saveQuotaSample(account: String, sample: it.pixelbox.cmwatch.rules.QuotaHistory.Sample) { samples += account to sample }
    override suspend fun loadQuotaSamples(since: Long) =
        samples.filter { it.second.ts >= since }.groupBy({ it.first }, { it.second }).mapValues { (_, v) -> v.sortedBy { it.ts } }
    override suspend fun pruneQuotaSamples(olderThan: Long) { samples.removeAll { it.second.ts < olderThan } }
    override suspend fun clearQuotaSamples(account: String) { samples.removeAll { it.first == account } }

}
