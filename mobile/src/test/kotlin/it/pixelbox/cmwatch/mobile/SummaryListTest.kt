package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/** Il riepilogo unico (design 03/10): i gruppi del bisogno, una riga aperta, molte sessioni ferme. */
class SummaryListTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private fun model(state: State) = Summary.build(state, emptyList(), emptyList(), state.ts, ZoneId.of("Europe/Rome"), emptySet())

    @Composable private fun page(state: State, open: String? = null) = CmPhoneTheme(still = true) {
        Column(Modifier.background(CmColors.bg)) {
            SummaryList(model(state), {}, { _, _ -> }, { _, _ -> }, {}, {}, initiallyOpen = open)
        }
    }

    @Test fun summaryClosed() = paparazzi.snapshot { page(st) }
    @Test fun summaryOpen() = paparazzi.snapshot { page(st, open = "WAITING:ledger-api") }

    // Attenzione n. 5 del piano: con dieci sessioni ferme «Ferme» resta una riga breve ciascuna, «Chiuse» una riga sola.
    @Test fun summaryMany() = paparazzi.snapshot {
        val idle = st.sessions.first { it.state == SessionState.IDLE }
        val many = (1..10).map { i -> idle.copy(id = "idle-$i", name = "progetto-$i", since = st.ts - i * 1800L) }
        page(st.copy(sessions = st.sessions + many))
    }

    // La master agganciata sopra il campo (design 03/10), e «Riapri la master» quando non c'è.
    @Test fun dockMaster() = paparazzi.snapshot {
        val m = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null)
        val reply = TranscriptEntry("a1", "assistant", "Lanciata claude-master sulla fase 2.2.\n\nEsito: Fase 2.2 avviata su claude-master\nProssimi: distilla il confronto nella kb", st.ts - 600)
        CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.bg)) { MasterDock(m, it.pixelbox.cmwatch.rules.MasterHome.hero(listOf(reply), m), {}, {}) } }
    }
    // La master espansa a tutta pagina (Franz, 03/10 16:44): la stessa barra in cima, con ▼ per ridurla.
    @Test fun dockMasterExpanded() = paparazzi.snapshot {
        val m = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null)
        val reply = TranscriptEntry("a1", "assistant", "Lanciata claude-master sulla fase 2.2.\n\nEsito: Fase 2.2 avviata su claude-master", st.ts - 600)
        CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.bg)) { MasterDock(m, it.pixelbox.cmwatch.rules.MasterHome.hero(listOf(reply), m), {}, {}, expanded = true) } }
    }
    @Test fun dockAbsent() = paparazzi.snapshot { CmPhoneTheme(still = true) { Column(Modifier.background(CmColors.bg).padding(16.dp)) { MasterAbsent {} } } }
}
