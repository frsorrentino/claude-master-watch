package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.RecapActions
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** Il Recap (mockup approvato l'08/10, contratto 1.46): la sezione in home e la pagina con filtri e gruppi. */
class RecapScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText()).let { s ->
        s.copy(recap = s.recap.copy(items = s.recap.items + RecapItem("orbit-docs", "API pages drafted", "Review the API pages")))
    }
    private val actions = RecapActions.of(st, "Riprendi il progetto %1\$s: %2\$s")
    private val agenda = AgendaPage(listOf(
        AgendaRow("aperto", "agenzia", "franz", "Confirm the 6 client ids with a candidate", ".claude/to-decide-client-id.md", detail = "Six ids have a likely client.\nConfirm them one by one."),
        AgendaRow("aperto", "personale", "franz", "Renew the domain of the docs site", "orbit-docs"),
        AgendaRow("aperto", "agenzia", "franz", "Empty the 46 containers with Universal Analytics", "off since July 2024"),
        AgendaRow("aperto", "personale", "claude", "Move the docs site to the new host", "orbit-docs"),
        AgendaRow("aperto", "postazione", "claude", "Clear the orphan plugin caches", "after restarting every session"),
        AgendaRow("aperto", "agenzia", "terzi", "Client answer on the domain", ""),
        AgendaRow("sospeso", "agenzia", "terzi", "Staging of atlas-shop not reachable from the CLI", "", until = "2026-10-20"),
        AgendaRow("scartato", "agenzia", "franz", "Old idea nobody wants", ""),
        AgendaRow("fatto", "postazione", "nessuno", "Backups of the workstation every night", "crontab"),
        AgendaRow("chiuso", "personale", "franz", "Old domain transfer", ""),
    ))

    @Test fun recapHome() = paparazzi.snapshot {
        val one = st.copy(sessions = st.sessions.filter { it.state == SessionState.IDLE }.take(1))
        val m = Summary.build(one, emptyList(), emptyList(), one.ts, ZoneId.of("Europe/Rome"), emptySet())
        CmPhoneTheme(still = true) {
            Column(Modifier.background(CmColors.bg)) {
                SummaryList(m, {}, { _, _ -> }, { _, _ -> }, {}, {}, startOpen = setOf("recap"), recapActions = actions, recapDate = st.recap.date, agenda = agenda, today = java.time.LocalDate.of(2026, 10, 9))
            }
        }
    }

    @Test fun recapPage() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { RecapScreen(actions, st.recap.date, agenda, error = null, loading = false, onBack = {}, onSend = {}, today = java.time.LocalDate.of(2026, 10, 9)) }
    }
}
