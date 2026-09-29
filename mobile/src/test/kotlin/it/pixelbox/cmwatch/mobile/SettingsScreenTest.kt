package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.mobile.ui.*

class SettingsScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    @Test fun settingsPaired() = paparazzi.snapshot { CmPhoneTheme(still = true) { SettingsScreen("penguin", "Pixel 9", "Pixel Watch 5", false, false, "0.2", {}, {}, {}, {}) } }
    @Test fun settingsDemo() = paparazzi.snapshot { CmPhoneTheme(still = true) { SettingsScreen(null, "Pixel 9", null, false, true, "0.2", {}, {}, {}, {}) } }
}
