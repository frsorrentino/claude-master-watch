package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class ContractTest {
    private val all = listOf(Fixtures.stateQuestion, Fixtures.stateIdle, Fixtures.stateStale)

    @Test fun stateOneDecodesEveryField() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        assertEquals(1, s.v); assertEquals("crostini-demo", s.host); assertEquals(4, s.sessions.size)
        val led = s.sessions[0]
        assertEquals("ledger-api", led.name); assertEquals(SessionState.WAITING, led.state)
        assertTrue(led.followed); assertEquals("Wait for the go", led.next)
        val q = led.question!!
        assertEquals(QuestionKind.ASK, q.kind); assertEquals(Tier.MEDIUM, q.tier)
        assertEquals(listOf(1, 2), q.options.map { it.n }); assertEquals("yes", q.options[0].label)
        val atlas = s.sessions[1]
        assertEquals("Bash pytest -q tests", atlas.tool); assertEquals(1789210300L, atlas.outcome!!.at)
        assertNull(s.quota.getValue("agenzia").h5); assertTrue(s.quota.getValue("agenzia").stale)
        assertEquals(11, s.quota.getValue("personale").h5)
        assertEquals(3, s.projects.size); assertEquals(2, s.night.queued); assertNull(s.night.running)
        assertEquals("2026-09-12", s.recap.date); assertEquals(2, s.recap.items.size)
    }

    @Test fun quotaCarriesTheFiveHourReset() {
        // Contratto 1.3: `reset_h5` è la ripartenza della finestra di 5 ore; `reset_w7` resta quella settimanale.
        val q = ContractJson.decodeState(Fixtures.stateQuestion).quota
        assertEquals(1789228800L, q.getValue("personale").resetH5)
        assertEquals(1789225200L, q.getValue("agenzia").resetH5)
        assertEquals(1789610400L, q.getValue("personale").resetW7)
    }

    @Test fun staleFixtureHasNoSessions() {
        val s = ContractJson.decodeState(Fixtures.stateStale)
        assertTrue(s.sessions.isEmpty()); assertEquals(1789200000L, s.ts)
    }

    @Test fun rulesHoldOnEveryFixture() {
        for (raw in all) {
            assertTrue("state ≤ 8 KB", raw.toByteArray().size <= 8 * 1024)
            val s = ContractJson.decodeState(raw)
            for (ses in s.sessions) {
                ses.outcome?.let { assertTrue(it.short.length <= 200); assertTrue(it.full.length <= 600) }
                ses.question?.let { q ->
                    assertFalse(q.text.endsWith("…")); assertTrue(q.text.isNotBlank())
                    assertEquals((1..q.options.size).toList(), q.options.map { it.n })
                }
            }
            assertEquals(Order.sessions(s.sessions), s.sessions)
        }
    }

    @Test fun roundTripKeepsEveryValue() {
        for (raw in all) {
            val s = ContractJson.decodeState(raw)
            val again = ContractJson.decodeState(ContractJson.json.encodeToString(State.serializer(), s))
            assertEquals(s, again)
        }
    }

    @Test fun eventsDecodeOnePerKind() {
        val ev = ContractJson.decodeEvents(Fixtures.events)
        assertEquals(EventKind.entries.size - 1, ev.map { it.kind }.toSet().size) // manca «resumed» nella fixture
        assertEquals("q-1789210500-1", ev.first { it.kind == EventKind.QUESTION }.ref)
        assertNull(ev.first { it.kind == EventKind.QUOTA }.session)
    }

    @Test fun cmdAndResultDecodeAndEncode() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        assertEquals(CmdOp.entries.size - 1, cmds.map { it.op }.toSet().size) // manca «unfollow» nella fixture
        assertEquals(8, results.size); assertEquals(2, results.count { !it.ok })
        val enc = ContractJson.encode(cmds[0])
        assertTrue(enc.contains("\"op\":\"answer\"")); assertTrue(enc.contains("\"arg\":\"1\""))
        assertEquals(cmds[0], ContractJson.json.decodeFromString(Cmd.serializer(), enc))
        assertEquals(results[3], ContractJson.decodeResult(ContractJson.json.encodeToString(CmdResult.serializer(), results[3])))
    }

    @Test fun lastAsksForTheWholeReply() {
        // Contratto 1.4: «last» chiede al PC l'ultima risposta intera della sessione, per la lettura a voce.
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val last = cmds.single { ContractJson.encode(it).contains("\"op\":\"last\"") }
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
            .single { it.id == last.id }
        assertTrue(res.ok); assertTrue(res.text.isNotBlank())
        assertEquals(last, ContractJson.json.decodeFromString(Cmd.serializer(), ContractJson.encode(last)))
    }

    @Test fun unknownKeysAreIgnored() {
        val s = ContractJson.decodeState(Fixtures.stateIdle.replaceFirst("\"host\"", "\"extra\": 1, \"host\""))
        assertEquals("crostini-demo", s.host)
    }
}
