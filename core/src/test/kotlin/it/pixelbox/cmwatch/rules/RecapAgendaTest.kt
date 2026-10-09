package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
import it.pixelbox.cmwatch.contract.Cmd
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.contract.ContractJson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Le righe dell'agenda nel Recap (contratto 1.46, mockup approvato l'08/10).
class RecapAgendaTest {
    private val page: AgendaPage = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray
        .first { it.jsonObject.getValue("id").jsonPrimitive.content.endsWith("0386") }
        .let { ContractJson.decodeAgenda(it.jsonObject.getValue("text").jsonPrimitive.content) }

    @Test fun openRowsByWhoMustMoveAndTheRestBelow() {
        val m = RecapAgenda.of(page)
        assertEquals(listOf("Confirm the 6 client ids with a candidate"), m.you.map { it.title })
        assertEquals(listOf("Move the docs site to the new host"), m.claude.map { it.title })
        assertTrue(m.other.isEmpty())
        assertEquals(listOf("sospeso", "fatto"), m.rest.map { it.state })
    }

    @Test fun scopeFilterMarksAndUnknownValues() {
        val rows = page.rows + AgendaRow("chiuso", "personale", "franz", "Old thing", "") +
            AgendaRow("aperto", "Agenzia", "terzi", "Client answer on the domain", "") + AgendaRow("stato", "ambito", "blocca", "titolo", "rif")
        val m = RecapAgenda.of(AgendaPage(rows), scope = "agenzia")
        assertEquals(listOf("Client answer on the domain"), m.other.map { it.title })
        assertEquals(listOf("Staging of atlas-shop not reachable from the CLI"), m.rest.map { it.title })
        // Uno stato sconosciuto va in fondo, com'è; l'intestazione del TSV non è una riga.
        assertEquals(listOf("sospeso", "fatto", "chiuso"), RecapAgenda.of(AgendaPage(rows)).rest.map { it.state })
        assertEquals(RecapAgenda.Mark.AGENCY, RecapAgenda.mark("Agenzia")); assertEquals(RecapAgenda.Mark.DESK, RecapAgenda.mark("postazione"))
        assertEquals(RecapAgenda.Mark.PERSONAL, RecapAgenda.mark("personale")); assertEquals(RecapAgenda.Mark.NONE, RecapAgenda.mark("altro"))
    }

    @Test fun doItGoesToTheMasterWithTheReference() {
        val a = RecapAgenda.doIt(RecapAgenda.of(page).claude.single())
        assertEquals("master", a.to); assertTrue(a.viaMaster && a.agenda)
        assertEquals("Fai questo lavoro dell'agenda: Move the docs site to the new host (orbit-docs)", a.send)
        assertEquals("Fai questo lavoro dell'agenda: X", RecapAgenda.doIt(AgendaRow("aperto", "", "claude", "X", " ")).send)
    }
}

// Le Azioni sulle schede (piano approvato da Franz il 09/10, «Approvo, prosegui»; formato e contratto 1.47).
class RecapAgendaActionsTest {
    private val today = java.time.LocalDate.of(2026, 10, 9)
    private fun row(state: String = "aperto", blocks: String = "franz", ref: String = "", detail: String? = null, until: String? = null) =
        AgendaRow(state, "agenzia", blocks, "Revoke the API key", ref, detail, until, key = "k1")

    @Test fun postponedRowsComeBackOnTheirDayAndDiscardedOnesDisappear() {
        val page = AgendaPage(listOf(
            row("sospeso", until = "2026-10-09"), row("sospeso", until = "2026-10-10"), row("sospeso"),
            row("scartato"), row("chiuso"), row("fatto"),
        ))
        val m = RecapAgenda.of(page, today = today)
        assertEquals(1, m.you.size)
        assertEquals(listOf("sospeso", "sospeso", "chiuso", "fatto"), m.rest.map { it.state })
    }

    @Test fun menuHasTheWritesOnlyWithTheOpAndPassGoesTheOtherWay() {
        val k = RecapAgenda.Item.entries
        assertEquals(listOf(RecapAgenda.Item.DEEPEN, RecapAgenda.Item.DO, RecapAgenda.Item.TALK), RecapAgenda.menu(row(), canWrite = false))
        val full = RecapAgenda.menu(row(ref = "orbit-docs"), canWrite = true)
        assertEquals(k.filter { it != RecapAgenda.Item.PASS_ME }, full)
        assertTrue(RecapAgenda.Item.PASS_ME in RecapAgenda.menu(row(blocks = "claude"), canWrite = true))
        assertTrue(RecapAgenda.Item.PASS_CLAUDE !in RecapAgenda.menu(row(blocks = "claude"), canWrite = true))
        // Una scheda non aperta non si fa, non si rimanda e non si passa: si legge, se ne parla, si toglie.
        assertEquals(listOf(RecapAgenda.Item.DEEPEN, RecapAgenda.Item.TALK, RecapAgenda.Item.REMOVE), RecapAgenda.menu(row("fatto"), canWrite = true))
    }

    @Test fun textsReferencesAndDates() {
        assertEquals("Approfondisci: Revoke the API key (console.anthropic.com)", RecapAgenda.deepenText(row(ref = "console.anthropic.com")))
        assertEquals("Approfondisci: Revoke the API key", RecapAgenda.deepenText(row()))
        assertEquals("Sulla scheda «Revoke the API key» (x.md): ", RecapAgenda.talkText(row(ref = "x.md")))
        assertEquals("https://console.anthropic.com", RecapAgenda.url("console.anthropic.com -> API Keys"))
        assertEquals("https://example.com/a", RecapAgenda.url("https://example.com/a"))
        assertEquals(null, RecapAgenda.url(".claude/DA-DECIDERE-client-id.md"))
        assertEquals(null, RecapAgenda.url("orbit-docs"))
        assertEquals(null, RecapAgenda.url("tag-runtime.json"))
        assertEquals(java.time.LocalDate.of(2026, 10, 10), RecapAgenda.tomorrow(today))
        assertEquals(java.time.LocalDate.of(2026, 10, 12), RecapAgenda.nextWeek(today)) // venerdì: lunedì dopo
        assertEquals(java.time.LocalDate.of(2026, 10, 19), RecapAgenda.nextWeek(java.time.LocalDate.of(2026, 10, 12))) // lunedì: quello dopo
    }
}

// Contratto 1.47: le scritture diventano `agenda_set`, «tu» è l'owner dell'agenda.
class RecapAgendaSetTest {
    private val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
    private val page = root.getValue("result").jsonArray.first { it.jsonObject.getValue("id").jsonPrimitive.content.endsWith("0386") }
        .let { ContractJson.decodeAgenda(it.jsonObject.getValue("text").jsonPrimitive.content) }

    @Test fun ownerIsYouAndKeysArrive() {
        assertEquals("owner", page.owner)
        val m = RecapAgenda.of(page)
        assertEquals(listOf("Confirm the 6 client ids with a candidate"), m.you.map { it.title })
        assertTrue(page.rows.all { it.key.isNotBlank() })
        assertTrue(RecapAgenda.isYou("Franz", "franz")); assertTrue(!RecapAgenda.isYou("owner", "franz"))
    }

    @Test fun editsBecomeTheCommandsOfTheFixture() {
        val r = page.rows.first()
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val done = RecapAgenda.setCmd(RecapAgenda.Edit(RecapAgenda.Item.DONE, r), page.owner)!!
        val f = cmds.first { it.op == CmdOp.AGENDA_SET && it.arg == r.key && it.action == "done" }
        assertEquals(f.arg, done.key); assertEquals(f.action, done.action)
        assertEquals(RecapAgenda.SetCmd(r.key, "snooze", until = "2026-10-20"), RecapAgenda.setCmd(RecapAgenda.Edit(RecapAgenda.Item.POSTPONE, r, java.time.LocalDate.of(2026, 10, 20)), page.owner))
        assertEquals("owner", RecapAgenda.setCmd(RecapAgenda.Edit(RecapAgenda.Item.PASS_ME, r), page.owner)!!.blocks)
        assertEquals("claude", RecapAgenda.setCmd(RecapAgenda.Edit(RecapAgenda.Item.PASS_CLAUDE, r), page.owner)!!.blocks)
        assertEquals(null, RecapAgenda.setCmd(RecapAgenda.Edit(RecapAgenda.Item.DONE, r.copy(key = "")), page.owner))
    }
}
