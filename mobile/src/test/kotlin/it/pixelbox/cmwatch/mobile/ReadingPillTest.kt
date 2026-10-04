package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.ReadingPill
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Rule
import org.junit.Test

/** Il mini-controller della lettura con la velocità scelta e il tasto della voce (Franz, 04/10 20:28). */
class ReadingPillTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val text = "Ho corretto il tasto della velocità e aggiunto quello della voce."

    /** La velocità arriva dal parametro: 1,5× e non il valore predefinito del provider. */
    @Test fun rateAndVoice() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp)) {
                ReadingPill("fable-director", text, onOpen = {}, onStop = {}, rate = 1.5f, onRate = {}, voice = "it-it-x-itd-local", onVoice = {})
            }
        }
    }

    /** Una colonna stretta del tablet: restano i tasti, la riga del testo si vede già nella colonna. */
    @Test fun narrowColumn() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp).width(260.dp)) {
                ReadingPill("fable-director", text, onOpen = {}, onStop = {}, rate = 1.25f, onRate = {}, voice = null, onVoice = {})
            }
        }
    }

    /** Senza voci italiane da scegliere il tasto della voce non c'è. */
    @Test fun noVoices() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp)) {
                ReadingPill(null, text, onOpen = null, onStop = {}, rate = 1f, onRate = {}, voice = null, onVoice = null)
            }
        }
    }
}
