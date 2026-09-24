package it.pixelbox.cmwatch.mobile.pair

import it.pixelbox.cmwatch.pairing.*
import it.pixelbox.cmwatch.settings.Settings
import it.pixelbox.cmwatch.settings.SettingsStore
import it.pixelbox.cmwatch.transport.Rtdb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Base64

private class MemStore(var s: Settings = Settings()) : SettingsStore {
    override suspend fun current() = s
    override suspend fun update(block: (Settings) -> Settings) { s = block(s) }
}

private object PlainKeys : KeyWrap {
    override fun wrap(key: ByteArray): String = Base64.getEncoder().encodeToString(key)
    override fun unwrap(wrapped: String): ByteArray = Base64.getDecoder().decode(wrapped)
}

private object ThrowingKeys : KeyWrap {
    override fun wrap(key: ByteArray): String = throw IllegalStateException("keystore down")
    override fun unwrap(wrapped: String): ByteArray = throw IllegalStateException("keystore down")
}

private class FakeFirebase(var result: Ensure = Ensure.Ready("phoneUid")) : PhoneFirebase {
    override suspend fun ensure(cfg: FirebaseConfig) = result
    override fun rtdb(): Rtdb = error("non usato: il PC è finto")
}

/** Un orologio finto che risponde come PairReceiver, con il WatchHandoff vero. */
private class FakeWatch : WatchLink {
    var uid = "watchUid"; var restartsLeft = 0; var reachable = true; var connectedWithoutApp = false; var dropKey = false
    var badEph = false
    var received: ByteArray? = null
    private val handoff = WatchHandoff()
    private val node = WatchNode("n1", "Pixel Watch 5")
    override suspend fun find() = node.takeIf { reachable }
    override suspend fun anyConnected() = node.takeIf { reachable || connectedWithoutApp }
    override suspend fun openPlayOnWatch(node: WatchNode) = true
    override suspend fun request(node: WatchNode, path: String, body: ByteArray): ByteArray = when (path) {
        HandoffMessages.HELLO -> HandoffMessages.encode(HelloResponse.serializer(),
            if (restartsLeft > 0) { restartsLeft--; HelloResponse(restart = true) }
            else HelloResponse(uid = uid, name = "Pixel Watch 5", eph = if (badEph) "not-a-key" else handoff.open(uid)))
        HandoffMessages.KEY -> {
            if (dropKey) throw java.io.IOException("watch gone")
            received = handoff.take(HandoffMessages.decode(KeyRequest.serializer(), body)!!)
            HandoffMessages.encode(KeyResponse.serializer(), KeyResponse(ok = true))
        }
        else -> error(path)
    }
}

class PairingControllerTest {
    private val qrText = File("../contract/pair-qr.json").readText()
    private val qr = PairQr.parse(qrText)!!
    private val key = ByteArray(32) { 9 }
    private val store = MemStore()
    private val watch = FakeWatch()
    private val fb = FakeFirebase()
    private var pcWatch: WatchPeer? = null
    private var pcError: PairError? = null

    private fun controller(now: Long = qr.e - 60, keys: KeyWrap = PlainKeys) = PairingController(
        store, fb, watch, keys,
        pairer = { PcPairer { _, uid, _, w -> pcError?.let { throw it }; pcWatch = w; PhonePairResult(key, "penguin", listOfNotNull(uid, w?.uid)) } },
        phoneName = "Pixel 9", now = { now }, restartWaitMs = 0,
    )

    @Test fun phoneAndWatchPaired() = runTest {
        val c = controller()
        c.run(qrText)
        val ui = c.ui.value
        assertEquals(Phase.DONE, ui.phase)
        assertEquals(listOf(StepState.DONE, StepState.DONE, StepState.DONE), Step.entries.map { ui.steps[it] })
        assertEquals(WatchPeer("watchUid", "Pixel Watch 5"), pcWatch)
        assertArrayEquals(key, watch.received)
        val s = store.s
        assertTrue(s.paired); assertEquals("phoneUid", s.uid); assertEquals("penguin", s.host)
        assertArrayEquals(key, PlainKeys.unwrap(s.wrappedKey!!))
        assertEquals(PairingRecord(listOf("phoneUid", "watchUid"), mapOf("phoneUid" to "Pixel 9", "watchUid" to "Pixel Watch 5"), "watchUid", "Pixel Watch 5", watchPending = false),
            PairingRecord.fromJson(s.pairingJson))
        assertEquals(qr.f.config(), FirebaseConfig.fromJson(s.firebaseJson))
    }

    @Test fun notOurQrStopsBeforeTheNetwork() = runTest {
        val c = controller()
        c.run("https://example.com")
        assertEquals(PairFail.INVALID, c.ui.value.fail)
        assertNull(store.s.firebaseJson)
    }

    @Test fun expiredQr() = runTest {
        val c = controller(now = qr.e + 1)
        c.run(qrText)
        assertEquals(PairFail.EXPIRED, c.ui.value.fail)
    }

    @Test fun noWatchAsksThenPairsThePhoneAlone() = runTest {
        watch.reachable = false
        val c = controller()
        c.run(qrText)
        assertEquals(PairFail.NO_WATCH, c.ui.value.fail)
        assertFalse(store.s.paired)
        c.retry(withoutWatch = true)
        assertEquals(Phase.DONE, c.ui.value.phase)
        assertEquals(StepState.SKIPPED, c.ui.value.steps[Step.WATCH])
        assertNull(pcWatch)
        assertEquals(listOf("phoneUid"), PairingRecord.fromJson(store.s.pairingJson)!!.uids)
    }

    @Test fun watchWithoutTheApp() = runTest {
        watch.reachable = false; watch.connectedWithoutApp = true
        val c = controller()
        c.run(qrText)
        assertEquals(PairFail.WATCH_APP_MISSING, c.ui.value.fail)
    }

    @Test fun watchRestartsForAnotherProjectThenAnswers() = runTest {
        watch.restartsLeft = 1
        val c = controller()
        c.run(qrText)
        assertEquals(Phase.DONE, c.ui.value.phase)
        assertArrayEquals(key, watch.received)
    }

    @Test fun pcSilentIsAnError() = runTest {
        pcError = PairError.NoConfirm()
        val c = controller()
        c.run(qrText)
        assertEquals(PairFail.PC_NO_CONFIRM, c.ui.value.fail)
        assertFalse(store.s.paired)
    }

    @Test fun watchGoneAfterThePcKeepsTheKeyForLater() = runTest {
        watch.dropKey = true
        val c = controller()
        c.run(qrText)
        assertEquals(Phase.DONE, c.ui.value.phase)
        assertEquals(StepState.PENDING, c.ui.value.steps[Step.WATCH])
        assertTrue(PairingRecord.fromJson(store.s.pairingJson)!!.watchPending)
        watch.dropKey = false
        assertTrue(c.completePending())
        assertArrayEquals(key, watch.received)
        assertFalse(PairingRecord.fromJson(store.s.pairingJson)!!.watchPending)
    }

    @Test fun aWatchWithANewUidIsNotGivenTheKey() = runTest {
        watch.dropKey = true
        val c = controller()
        c.run(qrText)
        watch.dropKey = false; watch.uid = "someoneElse"
        assertFalse(c.completePending())
        assertNull(watch.received)
        assertEquals(PairFail.WATCH_UID_CHANGED, c.ui.value.fail)
    }

    /** Revisione finale, 1: un cambio di progetto butta l'accoppiamento vecchio invece di lasciarlo vivo su un database sbagliato. */
    @Test fun switchingProjectDropsTheOldPairing() = runTest {
        store.s = Settings(paired = true, uid = "oldUid", host = "oldpc", wrappedKey = "oldKey", pairingJson = PairingRecord(listOf("oldUid"), mapOf("oldUid" to "Pixel 9")).toJson(), firebaseJson = "old")
        fb.result = Ensure.Restart
        controller().run(qrText)
        assertFalse(store.s.paired); assertNull(store.s.wrappedKey); assertNull(store.s.pairingJson)
        assertEquals(qr.f.config(), FirebaseConfig.fromJson(store.s.firebaseJson))
    }

    /** Revisione finale, 3: il QR salvato per il riavvio si consuma alla lettura, anche se poi fallisce. */
    @Test fun resumeQrIsConsumedOnce() = runTest {
        store.s = Settings(resumeQr = qrText)
        fb.result = Ensure.Failed
        val c = controller()
        assertTrue(c.resume())
        assertNull(store.s.resumeQr)
        assertEquals(PairFail.NETWORK, c.ui.value.fail)
        assertFalse(c.resume())
    }

    /** Revisione finale, 2: una risposta rotta dell'orologio è un errore mostrato, non un crash. */
    @Test fun aBrokenWatchReplyIsAFailureNotACrash() = runTest {
        watch.badEph = true
        val c = controller()
        c.run(qrText)
        assertEquals(Phase.FAILED, c.ui.value.phase)
        assertEquals(PairFail.WATCH_FAILED, c.ui.value.fail)
        assertFalse(store.s.paired)
    }

    /** Revisione finale, 2: un Keystore che non risponde è un errore mostrato al passo PC, non un crash. */
    @Test fun aThrowingKeystoreIsAFailureNotACrash() = runTest {
        val c = controller(keys = ThrowingKeys)
        c.run(qrText)
        assertEquals(Phase.FAILED, c.ui.value.phase)
        assertEquals(PairFail.FAILED, c.ui.value.fail)
        assertEquals(StepState.FAILED, c.ui.value.steps[Step.PC])
        assertFalse(store.s.paired)
    }

    @Test fun anotherFirebaseProjectRestartsThePhone() = runTest {
        fb.result = Ensure.Restart
        val c = controller()
        c.run(qrText)
        assertEquals(Phase.RESTART, c.ui.value.phase)
        assertEquals(qrText, store.s.resumeQr)
        assertFalse(store.s.paired)
    }
}
