package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.transport.FakeTransport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

class OverviewScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    // Lo stato della Demo con un «adesso» fisso: la fixture riportata al suo stesso istante, campioni e «Oggi» compresi.
    private val now = 1789210800L
    private val fake = FakeTransport({ File("../contract/$it.json").readText() }, now = { now })
    private fun model(samples: Boolean = true) = runBlocking {
        PhoneOverview.build(
            fake.state.first(), fake.events.first(), if (samples) fake.demoQuotaSamples() else emptyMap(), now,
            ZoneId.of("Europe/Rome"), stale = false,
        )
    }

    @Test fun overview() = paparazzi.snapshot { CmPhoneTheme(still = true) { OverviewScreen(model(), Freshness.Fresh, {}, {}) } }

    @Test fun overviewNoSamples() =
        paparazzi.snapshot { CmPhoneTheme(still = true) { OverviewScreen(model(samples = false), Freshness.Fresh, {}, {}) } }

    @Test fun overviewLargeFont() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(locale = "it", fontScale = 1.3f))
        paparazzi.snapshot { CmPhoneTheme(still = true) { OverviewScreen(model(), Freshness.Fresh, {}, {}) } }
    }

    @Test fun overviewSmall() {
        paparazzi.unsafeUpdateConfig(DeviceConfig.PIXEL_5.copy(locale = "it", screenWidth = 720, screenHeight = 1280))
        paparazzi.snapshot { CmPhoneTheme(still = true) { OverviewScreen(model(), Freshness.Fresh, {}, {}) } }
    }
}
