package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.mobile.pair.StepState
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.art.*

class ScenesTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val m = Modifier.fillMaxWidth().height(320.dp)

    @Test fun scan() = paparazzi.snapshot { CmPhoneTheme { ScanScene(m) } }
    @Test fun linkWatchFailed() = paparazzi.snapshot { CmPhoneTheme { LinkScene(StepState.DONE, StepState.FAILED, StepState.WAIT, m) } }
    @Test fun paired() = paparazzi.snapshot { CmPhoneTheme { PairedScene(watch = true, m) } }
    @Test fun emptySessions() = paparazzi.snapshot { CmPhoneTheme { EmptySessionsScene(m) } }
    @Test fun emptyDiary() = paparazzi.snapshot { CmPhoneTheme { EmptyDiaryScene(m) } }
}
