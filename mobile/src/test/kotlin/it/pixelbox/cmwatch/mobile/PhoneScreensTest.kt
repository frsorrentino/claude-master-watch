package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.NotPairedScreen
import it.pixelbox.cmwatch.mobile.pair.*
import it.pixelbox.cmwatch.mobile.ui.PairingScreen
import it.pixelbox.cmwatch.mobile.ui.PairedScreen
import org.junit.Rule
import org.junit.Test

/** Le schermate del telefono (design 24/09). Si registrano in GitHub Actions: `./gradlew :mobile:recordPaparazziDebug`. */
class PhoneScreensTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    @Test fun notPaired() = paparazzi.snapshot { CmPhoneTheme { NotPairedScreen(onPair = {}, onPaste = {}) } }

    private fun steps(p: StepState, w: StepState, c: StepState) = mapOf(Step.PHONE to p, Step.WATCH to w, Step.PC to c)

    @Test fun pairingRunning() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.RUNNING, steps(StepState.DONE, StepState.WORKING, StepState.WAIT)), {}, {}, {}, {}, {}) }
    }
    @Test fun pairingNoWatch() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.FAILED, steps(StepState.DONE, StepState.FAILED, StepState.WAIT), fail = PairFail.NO_WATCH), {}, {}, {}, {}, {}) }
    }
    @Test fun pairingWatchAppMissing() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.FAILED, steps(StepState.DONE, StepState.FAILED, StepState.WAIT), fail = PairFail.WATCH_APP_MISSING), {}, {}, {}, {}, {}) }
    }
    @Test fun pairingDone() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.DONE, steps(StepState.DONE, StepState.DONE, StepState.DONE), host = "penguin", watchName = "Pixel Watch 5"), {}, {}, {}, {}, {}) }
    }
    @Test fun pairingDoneWatchPending() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.DONE, steps(StepState.DONE, StepState.PENDING, StepState.DONE), host = "penguin", watchName = "Pixel Watch 5"), {}, {}, {}, {}, {}) }
    }
    @Test fun paired() = paparazzi.snapshot { CmPhoneTheme { PairedScreen("penguin", "Pixel 9", "Pixel Watch 5", watchPending = false, onRepair = {}) } }
    @Test fun pairedWatchPending() = paparazzi.snapshot { CmPhoneTheme { PairedScreen("penguin", "Pixel 9", "Pixel Watch 5", watchPending = true, onRepair = {}) } }
}

/** Le schermate del telefono con i caratteri di sistema grandi (1,3): i testi degli errori e dei passi restano interi (notte del 24/09). */
class PhoneLargeFontTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it", fontScale = 1.3f), theme = "android:Theme.Material.NoActionBar")

    private fun steps(p: StepState, w: StepState, c: StepState) = mapOf(Step.PHONE to p, Step.WATCH to w, Step.PC to c)

    @Test fun notPairedLargeFont() = paparazzi.snapshot { CmPhoneTheme { NotPairedScreen(onPair = {}, onPaste = {}) } }
    @Test fun pairingNoWatchLargeFont() = paparazzi.snapshot {
        CmPhoneTheme { PairingScreen(PairUi(Phase.FAILED, steps(StepState.DONE, StepState.FAILED, StepState.WAIT), fail = PairFail.NO_WATCH), {}, {}, {}, {}, {}) }
    }
    @Test fun pairedWatchPendingLargeFont() = paparazzi.snapshot { CmPhoneTheme { PairedScreen("penguin", "Pixel 9", "Pixel Watch 5", watchPending = true, onRepair = {}) } }
}
