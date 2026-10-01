package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.widget.WidgetConfigScreen
import it.pixelbox.cmwatch.mobile.widget.WidgetPrefs
import it.pixelbox.cmwatch.rules.WidgetModel
import org.junit.Rule
import org.junit.Test
import java.io.File

/** La personalizzazione alla posa: la regia di default e un account con le sue colonne. */
class WidgetConfigScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())

    @Test fun widgetConfigBoard() = paparazzi.snapshot { CmPhoneTheme(still = true) { WidgetConfigScreen(st, WidgetPrefs.DEFAULT) {} } }

    @Test fun widgetConfigAccount() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            WidgetConfigScreen(st, WidgetPrefs.DEFAULT.copy(mode = WidgetModel.Mode.ACCOUNT, target = "personal", metrics = WidgetModel.defaults(WidgetModel.Mode.ACCOUNT))) {}
        }
    }
}
