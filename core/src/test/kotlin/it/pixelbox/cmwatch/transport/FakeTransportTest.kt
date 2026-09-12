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

    @Test fun duplicateIdReturnsSameResult() = runTest {
        val tr = t(); val c = Cmd("dup", CmdOp.ANSWER, "ledger-api", "2", clock, "test")
        assertEquals(tr.send(c), tr.send(c))
    }

    @Test fun eventsComeFromFixtureNewestFirst() = runTest {
        val ev = t().events.first()
        assertEquals(6, ev.size); assertTrue(ev[0].ts >= ev[1].ts)
    }

    @Test fun pairAcceptsAnySixDigits() = runTest {
        assertEquals("crostini-demo", t().pair("123456", "watch").host)
        assertThrows(TransportException.Network::class.java) { runBlocking { t().pair("12", "watch") } }
    }
}
