package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test

/** Contratto 1.5: la description del comando dice cosa fa la sessione meglio del comando grezzo (Franz, 14/09 13:00). */
class ToolNoteTest {
    private val l = ToolText.Labels(
        run = "esegue %1\$s", read = "legge %1\$s", edit = "modifica %1\$s", write = "scrive %1\$s",
        search = "cerca %1\$s", web = "cerca sul web", message = "scrive a un'altra sessione",
        delegate = "delega a un agente", plan = "aggiorna il piano", other = "usa %1\$s",
    )
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)

    @Test fun laFixturePortaLaDescriptionDelComando() =
        assertEquals("Run the test suite", st.sessions.first { it.toolNote != null }.toolNote)

    @Test fun conLaDescriptionSiLeggeQuella() =
        assertEquals("Run the plugin test suite", ToolText.describe("Run the plugin test suite", "Bash cd ~/Desktop/workspaces/personali/fable-director && pytest", l))

    @Test fun senzaDescriptionRestaLaFraseDelloStrumento() {
        assertEquals("legge TileTexts.kt", ToolText.describe(null, "Read core/src/main/kotlin/TileTexts.kt", l))
        assertEquals("legge TileTexts.kt", ToolText.describe("  ", "Read core/src/main/kotlin/TileTexts.kt", l))
    }

    @Test fun laTileDiceLaDescriptionDiChiLavora() {
        val b = st.sessions.first { it.state == SessionState.BUSY }.copy(tool = "Bash cd ~/x && pytest", toolNote = "Run the plugin test suite")
        assertEquals("Run the plugin test suite", TileTexts.activity(b, busy = true, running = "turno in corso", idle = "a riposo", now = st.ts, tools = l))
    }
}
