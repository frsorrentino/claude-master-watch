package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class FakeTransportTest {
    private val clock = 1789210800L + 30
    private fun t() = FakeTransport(load = { Fixtures.read("$it.json") }, now = { clock })

    @Test fun startsOnQuestionFixtureRebasedToNow() = runTest {
        val s = t().state.first()
        assertEquals(4, s.sessions.size)
        assertEquals(clock, s.ts)                                          // ts riportato a «adesso»
        assertEquals(1789210500L + 30, s.sessions[0].question!!.askedAt)   // stessi scarti relativi
    }

    @Test fun staleFixtureKeepsOldTs() = runTest {
        val tr = t(); tr.useFixture("state-3-stale")
        assertEquals(1789200000L, tr.state.first().ts)
    }

    @Test fun answerRemovesTheQuestionAndReports() = runTest {
        val tr = t()
        val r = tr.send(Cmd("u1", CmdOp.ANSWER, "ledger-api", "1", clock, "test"))
        assertTrue(r.ok); assertEquals("answered 1. yes", r.text)
        val s = tr.state.first()
        val led = s.sessions.first { it.name == "ledger-api" }
        assertNull(led.question); assertEquals(SessionState.BUSY, led.state)
        assertEquals("ledger-api", s.sessions[1].name)                     // atlas-shop (busy) prima per alfabeto
    }

    @Test fun answerOnUnknownSessionFails() = runTest {
        val r = t().send(Cmd("u2", CmdOp.ANSWER, "nope", "1", clock, "test"))
        assertFalse(r.ok)
    }

    @Test fun promptAndScreenAndFollow() = runTest {
        val tr = t()
        assertEquals("delivered", tr.send(Cmd("u3", CmdOp.PROMPT, "atlas-shop", "ciao", clock, "test")).text)
        assertTrue(tr.send(Cmd("u4", CmdOp.SCREEN, "atlas-shop", null, clock, "test")).text.lines().size in 1..30)
        tr.send(Cmd("u5", CmdOp.FOLLOW, "atlas-shop", null, clock, "test"))
        assertTrue(tr.state.first().sessions.first { it.name == "atlas-shop" }.followed)
        assertFalse(tr.state.first().sessions.first { it.name == "ledger-api" }.followed)
    }

    @Test fun lastReadsTheWholeReplyOrSaysThereIsNone() = runTest {
        // Contratto 1.4: il finto PC risponde a «last» con l'esito intero della sessione, come fa il relay col transcript.
        val tr = t()
        val s = tr.fetchState().sessions.first { it.outcome != null }
        val r = tr.send(Cmd("u6", CmdOp.LAST, s.name, null, clock, "test"))
        assertTrue(r.ok); assertEquals(s.outcome!!.full, r.text)
        val none = tr.send(Cmd("u7", CmdOp.LAST, "nope", null, clock, "test"))
        assertFalse(none.ok); assertEquals("nope: nessun messaggio da leggere", none.text)
    }

    @Test fun duplicateIdReturnsSameResult() = runTest {
        val tr = t(); val c = Cmd("dup", CmdOp.ANSWER, "ledger-api", "2", clock, "test")
        assertEquals(tr.send(c), tr.send(c))
    }

    @Test fun eventsComeFromFixtureNewestFirst() = runTest {
        val ev = t().events.first()
        // I 6 della fixture ci sono tutti, più i 14 sparsi della demo (16/09 16:05).
        assertEquals(6 + 14, ev.size); assertTrue(ev.zipWithNext().all { (a, b) -> a.ts >= b.ts })
        assertEquals(6, ev.count { !it.key.startsWith("demo-") })
    }

    @Test fun pairAcceptsAnySixDigits() = runTest {
        assertEquals("crostini-demo", t().pair("123456", "watch").host)
        assertThrows(TransportException.Network::class.java) { runBlocking { t().pair("12", "watch") } }
    }

    // ---- Demo per i video promozionali (Franz, 16/09 16:05): i dati finti devono sembrare di adesso. ----

    /** La finestra delle 5 ore è in corso, a due ore dalla fine: così il ritmo ha una linea e una proiezione da mostrare. */
    @Test fun laFinestraDelleCinqueOreEInCorso() = runTest {
        val q = t().state.first().quota.getValue("personal")
        assertEquals(clock + 2 * 3600, q.resetH5)
        assertTrue("la settimana riparte nel futuro", q.resetW7!! > clock)
    }

    @Test fun iProgettiSonoStatiUsatiDiRecente() = runTest {
        val p = t().state.first().projects.single { it.name == "atlas-shop" }
        assertEquals(1789210700L + 30, p.lastUsed)
    }

    /** «Oggi» ha colonne in più ore della giornata, mai nel futuro. */
    @Test fun gliEventiRiempionoLaGiornata() = runTest {
        val ev = t().events.first()
        assertTrue(ev.all { it.ts <= clock })
        assertTrue("almeno cinque ore diverse", ev.map { (clock - it.ts) / 3600 }.toSet().size >= 5)
    }

    /** I campioni del ritmo: dentro la finestra, crescenti, e l'ultimo è la quota di adesso. */
    @Test fun iCampioniDelRitmoSalgonoFinoAllaQuotaDiAdesso() = runTest {
        val tr = t()
        val q = tr.state.first().quota.getValue("personal")
        val c = tr.demoQuotaSamples().getValue("personal")
        assertTrue(c.size >= 4)
        assertEquals(q.h5, c.last().pct)
        assertTrue(c.zipWithNext().all { (a, b) -> a.ts < b.ts && a.pct <= b.pct })
        assertTrue(c.all { it.ts >= q.resetH5!! - 5 * 3600 && it.ts <= clock })
        assertNull("senza lettura delle 5 ore niente campioni", tr.demoQuotaSamples()["work"])
    }
}
