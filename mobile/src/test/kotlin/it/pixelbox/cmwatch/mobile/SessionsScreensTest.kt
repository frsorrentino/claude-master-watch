package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

class SessionsScreensTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private fun fixture(n: String) = ContractJson.decodeState(File("../contract/$n.json").readText())

    @Test fun sessionsQuestion() {
        val s = fixture("state-1-question")
        paparazzi.snapshot { CmPhoneTheme(still = true) { SessionsScreen(Snapshot(s, Freshness.Fresh), s.ts, {}, {}) } }
    }
    @Test fun sessionsStale() {
        val s = fixture("state-3-stale")
        paparazzi.snapshot { CmPhoneTheme(still = true) { SessionsScreen(Snapshot(s, Freshness.Stale(6)), s.ts + 360, {}, {}) } }
    }
    @Test fun sessionsEmpty() {
        val s = fixture("state-2-idle").copy(sessions = emptyList())
        paparazzi.snapshot { CmPhoneTheme(still = true) { SessionsScreen(Snapshot(s, Freshness.Fresh), s.ts, {}, {}) } }
    }
    @Test fun sessionsLargeFont() {
        val s = fixture("state-1-question")
        paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", fontScale = 1.3f))
        paparazzi.snapshot { CmPhoneTheme(still = true) { SessionsScreen(Snapshot(s, Freshness.Fresh), s.ts, {}, {}) } }
    }
}
