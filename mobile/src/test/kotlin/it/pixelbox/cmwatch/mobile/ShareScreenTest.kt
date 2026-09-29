package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

class ShareScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())

    @Test fun shareText() = paparazzi.snapshot { CmPhoneTheme(still = true) { ShareScreen(st, "il pulsante del checkout è grigio", hasImage = false, sending = false, onSend = { _, _ -> }) } }
    @Test fun shareImage() = paparazzi.snapshot { CmPhoneTheme(still = true) { ShareScreen(st, "", hasImage = true, sending = false, onSend = { _, _ -> }) } }
    @Test fun shareOldRelay() = paparazzi.snapshot { CmPhoneTheme(still = true) { ShareScreen(st.copy(share = null), "", hasImage = true, sending = false, onSend = { _, _ -> }) } }
}
