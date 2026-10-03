package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.StartRoute.Tab

class ShellScreensTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    // Il riepilogo unico (design 03/10): «Sessioni · N aperte» in alto, niente schede in basso.
    @Test fun shellOverview() = paparazzi.snapshot { CmPhoneTheme(still = true) { AppShell(Tab.OVERVIEW, demo = false, {}, {}, openCount = 3) { Text("contenuto") } } }
    @Test fun shellDemo() = paparazzi.snapshot { CmPhoneTheme(still = true) { AppShell(Tab.DIARY, demo = true, {}, {}) { Text("contenuto") } } }
    // Il menu a pannello (tavola 5): collegamento, quattro voci spiegate, Impostazioni.
    @Test fun menuOpen() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { AppShell(Tab.OVERVIEW, demo = false, {}, {}, onSearch = {}, host = "penguin", updated = "aggiornato ora", openCount = 3, menuStartOpen = true) { Text("contenuto") } }
    }

    // Il menu delle sessioni al posto del titolo, con una scheda aperta: niente schede in basso.
    @Test fun shellSheetOpen() = paparazzi.snapshot {
        val st = it.pixelbox.cmwatch.contract.ContractJson.decodeState(java.io.File("../contract/state-1-question.json").readText())
        CmPhoneTheme(still = true) { AppShell(Tab.OVERVIEW, demo = false, {}, {}, sessions = st.sessions, current = st.sessions.first().name) { Text("contenuto") } }
    }
}
