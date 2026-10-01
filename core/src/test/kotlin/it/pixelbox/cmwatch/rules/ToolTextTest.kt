package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

class ToolTextTest {
    private val l = ToolText.Labels(
        run = "esegue %1\$s", read = "legge %1\$s", edit = "modifica %1\$s", write = "scrive %1\$s",
        search = "cerca %1\$s", web = "cerca sul web", message = "scrive a un'altra sessione",
        delegate = "delega a un agente", plan = "aggiorna il piano", other = "usa %1\$s",
    )

    @Test fun ilNomeNudoDiventaUnaFrase() {
        assertEquals("scrive a un'altra sessione", ToolText.phrase("SendMessage", l))
        assertEquals("delega a un agente", ToolText.phrase("Task", l))
        assertEquals("aggiorna il piano", ToolText.phrase("TodoWrite", l))
        assertEquals("cerca sul web", ToolText.phrase("WebFetch https://esempio.it/pagina", l))
    }

    @Test fun ilComandoRestaPerEsteso() {
        assertEquals("esegue pytest -q tests", ToolText.phrase("Bash pytest -q tests", l))
    }

    @Test fun deiPercorsiRestaSoloIlFile() {
        assertEquals("legge TileTexts.kt", ToolText.phrase("Read core/src/main/kotlin/it/pixelbox/cmwatch/rules/TileTexts.kt", l))
        assertEquals("modifica Notifier.kt", ToolText.phrase("Edit(wear/src/main/kotlin/it/pixelbox/cmwatch/wear/push/Notifier.kt)", l))
    }

    @Test fun unoStrumentoSconosciutoSiDiceComunque() {
        assertEquals("usa Paparazzi", ToolText.phrase("Paparazzi", l))
    }

    @Test fun nienteDaDireRestaNiente() {
        assertNull(ToolText.phrase(null, l))
        assertNull(ToolText.phrase("   ", l))
    }

    // Righe ricche dei passaggi (Franz, 01/10 15:59): icona per tipo, testo in chiaro e sotto comando o cartella.
    @Test fun kindOfTools() {
        assertEquals(ToolText.Kind.RUN, ToolText.kind("Bash"))
        assertEquals(ToolText.Kind.READ, ToolText.kind("Read"))
        assertEquals(ToolText.Kind.EDIT, ToolText.kind("MultiEdit"))
        assertEquals(ToolText.Kind.WRITE, ToolText.kind("Write"))
        assertEquals(ToolText.Kind.SEARCH, ToolText.kind("Grep"))
        assertEquals(ToolText.Kind.OTHER, ToolText.kind(null))
    }

    @Test fun bashRowShowsTheDescriptionThenTheCommand() {
        assertEquals("Trova la cartella" to "grep -n source cm-config.py", ToolText.row("Bash", "grep -n source cm-config.py", "Trova la cartella"))
        assertEquals("ls -la" to null, ToolText.row("Bash", "ls -la", null))
    }

    @Test fun fileRowShowsTheNameThenTheFolder() {
        assertEquals("cm-quota.py" to "claude-master/scripts", ToolText.row("Read", "claude-master/scripts/cm-quota.py", null))
        assertEquals("notes.py" to null, ToolText.row("Edit", "notes.py", null))
    }

    // Segnalazione 01/10 18:06: gli strumenti MCP arrivano senza testo né nota e la riga aperta restava vuota, e il
    // nome grezzo «mcp__chrome-bridge__execute_js» andava a capo nell'intestazione.
    @Test fun mcpToolsGoByTheirShortName() {
        assertEquals("execute_js", ToolText.short("mcp__chrome-bridge__execute_js"))
        assertEquals("Bash", ToolText.short("Bash"))
        assertEquals("execute_js" to null, ToolText.row("mcp__chrome-bridge__execute_js", "", null))
        assertEquals("?" to null, ToolText.row(null, null, null))
    }

    @Test fun browserMcpToolsAreWeb() {
        assertEquals(ToolText.Kind.WEB, ToolText.kind("mcp__chrome-bridge__click"))
        assertEquals(ToolText.Kind.WEB, ToolText.kind("mcp__claude-in-chrome__navigate"))
        assertEquals(ToolText.Kind.OTHER, ToolText.kind("mcp__firebase__auth_get_users"))
    }
}
