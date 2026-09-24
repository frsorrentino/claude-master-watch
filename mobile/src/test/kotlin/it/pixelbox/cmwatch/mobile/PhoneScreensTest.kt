package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.NotPairedScreen
import org.junit.Rule
import org.junit.Test

/** Le schermate del telefono (design 24/09). Si registrano in GitHub Actions: `./gradlew :mobile:recordPaparazziDebug`. */
class PhoneScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    @Test fun notPaired() = paparazzi.snapshot { CmPhoneTheme { NotPairedScreen(onPair = {}, onPaste = {}) } }
}
