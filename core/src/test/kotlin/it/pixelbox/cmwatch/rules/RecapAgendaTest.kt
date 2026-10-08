package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow
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
