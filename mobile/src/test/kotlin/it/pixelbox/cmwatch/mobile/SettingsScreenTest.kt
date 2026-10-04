package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.rules.SettingsDevices
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

/** Le Impostazioni del mockup A (Franz, 03/10 21:59): sezioni per argomento e lo schema dei dispositivi da toccare. */
class SettingsScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private fun devices(pending: Boolean = false, near: Boolean? = true, fresh: Freshness = Freshness.Fresh) =
        SettingsDevices.build("penguin", st, fresh, st.ts, "Pixel 9", "0.2", true, "Pixel Watch 5", pending, near)

    @Test fun settingsPaired() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { SettingsScreen("penguin", "Pixel 9", "Pixel Watch 5", false, false, "0.2", {}, {}, {}, {}, devices = devices()) }
    }

    // Contratto 1.32, variante B (Franz, 04/10 17:11): il PC sopra e i dispositivi veri in fila sotto, «questo» è il telefono;
    // il Chromebook letto ore fa in arancio, il Pixel 7 mai arrivato tratteggiato.
    @Test fun settingsDevicesB() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SettingsScreen(
                "penguin", "Pixel 9", "Pixel Watch 5", false, false, "0.2", {}, {}, {}, {}, devices = devices(),
                linked = SettingsDevices.linked(st, "phoneUid00000000000000000000", st.ts),
            )
        }
    }

    // L'orologio senza chiave: il suo filo e il suo punto in arancio, la scheda dice cosa manca.
    @Test fun settingsWatchPending() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            SettingsScreen("penguin", "Pixel 9", "Pixel Watch 5", true, false, "0.2", {}, {}, {}, {}, devices = devices(pending = true, near = null, fresh = Freshness.Stale(12)), initialDevice = DeviceNode.WATCH)
        }
    }

    @Test fun settingsDemo() = paparazzi.snapshot { CmPhoneTheme(still = true) { SettingsScreen(null, "Pixel 9", null, false, true, "0.2", {}, {}, {}, {}) } }
}
