package it.pixelbox.cmwatch.mobile

import androidx.compose.material3.Text
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.mobile.ui.*

class ShellScreensTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    @Test fun shellSessions() = paparazzi.snapshot { CmPhoneTheme(still = true) { AppShell(Tab.SESSIONS, demo = false, {}, {}) { Text("contenuto") } } }
    @Test fun shellDemo() = paparazzi.snapshot { CmPhoneTheme(still = true) { AppShell(Tab.DIARY, demo = true, {}, {}) { Text("contenuto") } } }
}
