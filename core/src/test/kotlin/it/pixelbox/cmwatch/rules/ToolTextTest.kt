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
}
