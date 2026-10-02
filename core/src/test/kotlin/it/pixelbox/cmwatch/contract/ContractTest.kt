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
        assertNull(s.quota.getValue("work").h5); assertTrue(s.quota.getValue("work").stale)
        assertEquals(11, s.quota.getValue("personal").h5)
        assertEquals(3, s.projects.size); assertEquals(2, s.night.queued); assertNull(s.night.running)
        assertEquals("2026-09-12", s.recap.date); assertEquals(2, s.recap.items.size)
    }

    @Test fun quotaCarriesTheFiveHourReset() {
        // Contratto 1.3: `reset_h5` è la ripartenza della finestra di 5 ore; `reset_w7` resta quella settimanale.
        val q = ContractJson.decodeState(Fixtures.stateQuestion).quota
        assertEquals(1789228800L, q.getValue("personal").resetH5)
        assertEquals(1789225200L, q.getValue("work").resetH5)
        assertEquals(1789610400L, q.getValue("personal").resetW7)
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
        // Contratto 1.12: due risultati in più, «model» riuscito ed «effort» rifiutato su una sessione occupata.
        // Contratto 1.13: uno in più, il launch con il primo messaggio. Contratto 1.17: night_add e night_remove.
        // Contratto 1.19: tre report, uno rifiutato. Contratto 1.21: due interrupt, «stopped» e «nothing to stop».
        // Contratto 1.22: due transcript, la prima pagina e una pagina `after`, e un prompt con `device`.
        // Contratto 1.24: due file, uno aperto e uno rifiutato perché non è nella trascrizione.
        // Contratto 1.25: due slash, /compact con testo e un comando non consentito.
        assertEquals(26, results.size); assertEquals(7, results.count { !it.ok })
        val slash = cmds.filter { it.op == CmdOp.SLASH }
        assertEquals(listOf("compact" to "sent /compact to field-notes", "model" to "not allowed: model"), slash.map { c -> c.arg to results.first { it.id == c.id }.text })
        assertNotNull(slash[0].text)
        assertEquals(listOf("compact", "clear", "exit", "context", "cost"), ContractJson.decodeState(Fixtures.stateIdle).slash)
        assertEquals(listOf("file ready (image/png, 69 bytes)", "not in the transcript"), cmds.indices.filter { cmds[it].op == CmdOp.FILE }.map { i -> results.first { it.id == cmds[i].id }.text })
        val enc = ContractJson.encode(cmds[0])
        assertTrue(enc.contains("\"op\":\"answer\"")); assertTrue(enc.contains("\"arg\":\"1\""))
        assertEquals(cmds[0], ContractJson.json.decodeFromString(Cmd.serializer(), enc))
        assertEquals(results[3], ContractJson.decodeResult(ContractJson.json.encodeToString(CmdResult.serializer(), results[3])))
    }

    /**
     * Contratto 1.13 (Franz, 16/09 14:33, «Nuova sessione» dal polso): `launch` porta il primo messaggio in `text`, il
     * `/result` dice il nome della sessione nata in `session`, e ogni progetto ha `last_used` per ordinarli dal più recente.
     */
    @Test fun launchWithFirstMessageNamesTheNewSession() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val launch = cmds.single { it.op == CmdOp.LAUNCH && it.text != null }
        assertEquals("check the draft for typos", launch.text)
        val r = results.single { it.id == launch.id }
        assertTrue(r.ok); assertEquals("field-notes-2", r.session)
        // Senza `text` il cmd si scrive come prima: un relay vecchio non vede campi nuovi.
        assertFalse(ContractJson.encode(cmds[0]).contains("\"text\""))
        val state = ContractJson.decodeState(Fixtures.stateQuestion)
        assertEquals(1789210700L, state.projects.single { it.name == "atlas-shop" }.lastUsed)
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

    @Test fun reopenBringsBackAGoneSession() {
        // Contratto 1.9: «reopen» rilancia una sessione gone nella sua cartella; `resume` resta la spinta a una viva.
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val reopen = cmds.single { it.op == CmdOp.REOPEN }
        assertEquals("orbit-docs", reopen.session); assertNull(reopen.arg)
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
            .single { it.id == reopen.id }
        assertTrue(res.ok); assertTrue(res.text.startsWith("reopened orbit-docs"))
        assertTrue(ContractJson.encode(reopen).contains("\"op\":\"reopen\""))
    }

    // Contratto 1.11 (16/09): ogni sessione porta modello, effort e contesto, letti dalla sua trascrizione. I campi ci
    // sono sempre e valgono null quando non si leggono: una sessione sparita non ha trascrizione, quindi niente numeri.
    @Test fun sessionsCarryModelEffortAndContext() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val ledger = s.sessions.single { it.name == "ledger-api" }
        assertEquals("claude-opus-5[1m]", ledger.model!!.id)
        assertEquals("Opus 5", ledger.model!!.label)
        assertEquals("high", ledger.effort)
        assertEquals(62, ledger.context)
        val atlas = s.sessions.single { it.name == "atlas-shop" }
        assertEquals("Sonnet 5", atlas.model!!.label)
        assertEquals(18, atlas.context)
        val gone = s.sessions.single { it.state == SessionState.GONE }
        assertNull(gone.model); assertNull(gone.effort); assertNull(gone.context)
    }

    // Contratto 1.16 (25/09): ogni sessione porta `low_priority` («off», «offered», «active», null se non si legge) e
    // `goal` (la condizione di completamento data con /goal: testo e da quando, null senza obiettivo).
    @Test fun sessionsCarryLowPriorityAndGoal() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val atlas = s.sessions.single { it.name == "atlas-shop" }
        assertEquals("active", atlas.lowPriority)
        assertEquals("All checkout tests green and the release tagged", atlas.goal!!.text)
        assertEquals(1789210700L, atlas.goal!!.since)
        assertEquals(false, atlas.goal!!.met)
        assertEquals("offered", s.sessions.single { it.name == "ledger-api" }.lowPriority)
        val notes = s.sessions.single { it.name == "field-notes" }
        assertEquals("off", notes.lowPriority); assertNull(notes.goal)
        val gone = s.sessions.single { it.state == SessionState.GONE }
        assertNull(gone.lowPriority); assertNull(gone.goal)
        // senza i campi (relay precedente) restano null
        val old = ContractJson.decodeState(Fixtures.stateQuestion.replace("\"low_priority\": \"active\",", "").replace(Regex(",\\s*\"goal\": \\{[^}]*\\}"), ""))   // `goal` è l'ultima chiave: via anche la virgola prima
        assertNull(old.sessions.single { it.name == "atlas-shop" }.lowPriority)
        assertNull(old.sessions.single { it.name == "atlas-shop" }.goal)
    }

    @Test fun unknownKeysAreIgnored() {
        val s = ContractJson.decodeState(Fixtures.stateIdle.replaceFirst("\"host\"", "\"extra\": 1, \"host\""))
        assertEquals("crostini-demo", s.host)
    }

    // Contratto 1.17 (29/09, richiesta R3 dell'app): la coda della notte modificabile. `night.items` c'è sempre per un
    // relay che la supporta, anche vuota; assente = relay precedente, e l'app lo dice invece di fallire.
    @Test fun nightQueueItemsAndCommands() {
        val s = ContractJson.decodeState(Fixtures.stateQuestion)
        val items = s.night.items!!
        assertEquals(listOf("a3f09c1e", "7b21d4e8"), items.map { it.id })
        assertEquals("ledger-api", items[1].name); assertNull(items[1].started); assertEquals(1789210620L, items[1].added)
        assertTrue(items.all { !it.prompt.contains("…") && !it.prompt.contains("\n") })
        assertEquals(emptyList<NightItem>(), ContractJson.decodeState(Fixtures.stateIdle).night.items)
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val add = cmds.single { it.op == CmdOp.NIGHT_ADD }
        assertEquals("/home/demo/workspaces/work/clients/ledger-api", add.arg); assertTrue(add.text!!.isNotBlank())
        assertEquals("7b21d4e8", res.single { it.id == add.id }.job)
        val remove = cmds.single { it.op == CmdOp.NIGHT_REMOVE }
        assertEquals("5d0e6b92", remove.arg); assertTrue(res.single { it.id == remove.id }.ok)
        assertTrue(ContractJson.encode(add).contains("\"op\":\"night_add\""))
    }

    @Test fun olderRelayHasNoNightItems() {
        val old = Fixtures.stateIdle.replace(Regex(",\\s*\"items\"\\s*:\\s*\\[\\s*\\]"), "")
        assertNull(ContractJson.decodeState(old).night.items)
    }

    // Contratto 1.18 (29/09, richiesta R4): diario delle 20:00 e resoconto della notte arrivano anche all'app come eventi.
    @Test fun recapAndNightReportEvents() {
        val ev = ContractJson.decodeEvents(Fixtures.read("events-sample.json"))
        val recap = ev.single { it.kind == EventKind.RECAP }
        assertNull(recap.session); assertNull(recap.account); assertTrue(recap.body.isNotBlank()); assertNotNull(recap.ref)
        val night = ev.single { it.kind == EventKind.NIGHT_REPORT }
        assertTrue(night.title.isNotBlank()); assertNotNull(night.ref)
        assertTrue(ev.any { it.kind == EventKind.QUOTA && it.title.startsWith("✓") })
        assertTrue(ev.all { !it.body.contains("…") && !it.title.contains("\n") })
    }

    // Contratto 1.19 (29/09, richiesta R5): «Condividi» verso una sessione. `share` nello stato dice che il relay lo supporta.
    @Test fun reportAndShareSignal() {
        assertEquals(1_500_000, ContractJson.decodeState(Fixtures.stateIdle).share!!.maxBytes)
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val res = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val reports = cmds.filter { it.op == CmdOp.REPORT }
        assertEquals(3, reports.size)
        assertNotNull(reports[0].arg); assertNull(reports[1].arg)
        assertEquals("sent to field-notes", res.single { it.id == reports[1].id }.text)
        assertFalse(res.single { it.id == reports[2].id }.ok)
    }

    @Test fun olderRelayHasNoShare() {
        val old = Fixtures.stateIdle.replace(Regex(",\\s*\"share\"\\s*:\\s*\\{[^}]*\\}"), "")
        assertNull(ContractJson.decodeState(old).share)
    }

    /** Contratto 1.22: `device` dice al relay se il prompt arriva dal telefono o dall'orologio; assente, non si scrive. */
    @Test fun promptCarriesTheDevice() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmd = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }.last { it.op == CmdOp.PROMPT }
        assertEquals("phone", cmd.device)
        assertTrue(ContractJson.encode(cmd).contains("\"device\":\"phone\""))
        assertFalse(ContractJson.encode(cmd.copy(device = null)).contains("device"))
    }
}
