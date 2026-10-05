package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.SettingsDevices.Tone
import org.junit.Assert.*
import org.junit.Test

/** Lo schema dei dispositivi nelle Impostazioni (mockup A, Franz 03/10 21:59): stato di ogni nodo e dei due fili. */
class SettingsDevicesTest {
    private val now = 1_790_000_000L
    private fun s(name: String, st: SessionState = SessionState.IDLE) =
        Session(id = name, name = name, account = "personale", project = name, state = st, since = 0)
    private val state = State(
        v = 1, ts = now, host = "penguin",
        sessions = listOf(s("master"), s("app"), s("relay", SessionState.BUSY), s("old", SessionState.GONE)),
        quota = mapOf("professionale" to QuotaAccount(h5 = 2, stale = true, kind = "work"), "personale" to QuotaAccount(h5 = 7, kind = "personal")),
    )
    private fun build(
        host: String? = "penguin", st: State? = state, fresh: Freshness? = Freshness.Fresh,
        watch: String? = "watch-pixel5", pending: Boolean = false, reachable: Boolean? = true,
    ) = SettingsDevices.build(host, st, fresh, now, "Pixel 11 Pro XL", "0.2", notifications = true, watchName = watch, watchPending = pending, watchReachable = reachable)

    @Test fun allLiveWhenTheStateIsFreshAndTheWatchIsNear() {
        val m = build()
        assertEquals(listOf(Tone.LIVE, Tone.LIVE, Tone.LIVE), listOf(m.phone.tone, m.pc.tone, m.watch.tone))
        assertEquals(Tone.LIVE, m.pcLink); assertEquals(Tone.LIVE, m.watchLink)
    }

    // Le sessioni aperte come nella home («Master · 2 aperte»): senza la master e senza le chiuse.
    @Test fun thePcCountsOpenSessionsLikeTheHome() {
        val pc = build().pc
        assertEquals(2, pc.open); assertEquals("penguin", pc.host)
        assertEquals(listOf("personale", "professionale"), pc.accounts.map { it.account })
        assertEquals(listOf(7, 2), pc.accounts.map { it.pct }); assertEquals(listOf(false, true), pc.accounts.map { it.stale })
    }

    @Test fun anOldStateTurnsThePcAndItsWireAmber() {
        val m = build(fresh = Freshness.Stale(12))
        assertEquals(Tone.STALE, m.pc.tone); assertEquals(Tone.STALE, m.pcLink); assertEquals(12, m.pc.ageMinutes)
    }

    @Test fun aWatchWaitingForItsKeyOrOutOfReachIsAmber() {
        assertEquals(Tone.STALE, build(pending = true).watch.tone)
        assertEquals(Tone.STALE, build(reachable = false).watch.tone)
        assertEquals(Tone.STALE, build(reachable = false).watchLink)
        // Non ancora saputo (la ricerca del nodo è in corso): non si accusa l'orologio.
        assertEquals(Tone.LIVE, build(reachable = null).watch.tone)
    }

    @Test fun unpairedIsGreyEverywhereButThePhone() {
        val m = build(host = null, st = null, fresh = null, watch = null, reachable = null)
        assertFalse(m.paired)
        assertEquals(Tone.LIVE, m.phone.tone)
        assertEquals(listOf(Tone.OFF, Tone.OFF, Tone.OFF, Tone.OFF), listOf(m.pc.tone, m.watch.tone, m.pcLink, m.watchLink))
    }

    @Test fun pairedWithoutAWatchLeavesItsWireOff() {
        val m = build(watch = null, reachable = null)
        assertTrue(m.paired); assertEquals(Tone.OFF, m.watch.tone); assertEquals(Tone.OFF, m.watchLink)
    }

    // Contratto 1.32, variante B (Franz, 04/10 17:11): i dispositivi veri dallo stato; «questo» è il proprio uid; verde se
    // ha letto da mezz'ora al massimo, arancio se da più tempo, spento se mai.
    @Test fun linkedDevicesFromTheState() {
        val st = it.pixelbox.cmwatch.contract.ContractJson.decodeState(java.io.File("../contract/state-2-idle.json").readText())
        val now = st.devices!!.first().seen!! + 10
        val l = SettingsDevices.linked(st, "tabletUid0000000000000000000", now)!!
        // Contratto 1.39: in fondo un browser, «Safari on Mac».
        assertEquals(listOf("Pixel 9", "Pixel Watch 5", "Pixel Tablet", "Chromebook", "Pixel 7", "Safari on Mac"), l.map { it.name })
        assertEquals(listOf(false, false, true, false, false, false), l.map { it.self })
        assertEquals(SettingsDevices.Tone.LIVE, l[0].tone)
        assertEquals(SettingsDevices.Tone.STALE, l[3].tone)
        assertEquals(SettingsDevices.Tone.OFF, l[4].tone)
        assertEquals(SettingsDevices.Tone.LIVE, l[2].tone)
    }

    @Test fun beforeTheContractThereIsNoList() = assertNull(SettingsDevices.linked(it.pixelbox.cmwatch.contract.State(v = 1, ts = 0, host = "pc"), "u", 0))
}
