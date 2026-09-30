package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.mobile.ui.*

class TerminalScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val sample = "● Read(core/src/Repo.kt)\n  ⎿  Read 240 lines\n\n● I'll add the phone board next.\n\n> "

    @Test fun terminalText() = paparazzi.snapshot { CmPhoneTheme(still = true) { TerminalScreen("kb", sample, loading = false, onRefresh = {}) } }
    @Test fun terminalLoading() = paparazzi.snapshot { CmPhoneTheme(still = true) { TerminalScreen("kb", sample, loading = true, onRefresh = {}) } }
    @Test fun terminalEmpty() = paparazzi.snapshot { CmPhoneTheme(still = true) { TerminalScreen("kb", null, loading = false, onRefresh = {}) } }
}
