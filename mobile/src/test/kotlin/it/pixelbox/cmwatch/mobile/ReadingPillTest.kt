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
import it.pixelbox.cmwatch.mobile.ui.LocalReadingOverlay
import it.pixelbox.cmwatch.mobile.ui.ReadingOverlay
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

    /** In pausa (Franz, 07/10 22:06): barrette ferme, «in pausa» sopra il testo, ▶ pieno per riprendere. */
    @Test fun paused() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp)) {
                ReadingPill("fable-director", text, onOpen = {}, onStop = {}, rate = 1.5f, onRate = {}, voice = "it-it-x-itd-local", onVoice = {}, paused = true)
            }
        }
    }

    /** La velocità aperta: il valore pieno, lo slider da 0,5× a 2× con il segno di 1×, poi pausa e stop. */
    @Test fun rateOpen() = paparazzi.snapshot {
        val overlay = ReadingOverlay().apply { rateOpen = true }
        CmPhoneTheme(still = true) {
            androidx.compose.runtime.CompositionLocalProvider(LocalReadingOverlay provides overlay) {
                Box(Modifier.background(CmColors.bg).padding(12.dp)) {
                    ReadingPill("fable-director", text, onOpen = {}, onStop = {}, rate = 1.35f, onRate = {}, voice = "it-it-x-itd-local", onVoice = {})
                }
            }
        }
    }

    // Il pannello della live (Franz, 08/10 20:03): mentre parla, e in pausa senza auricolari.
    @Test fun liveSpeaking() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp)) {
                it.pixelbox.cmwatch.mobile.ui.LivePill("ledger-api ha finito: migrazione committata in locale.", speaking = true, paused = false, noHeadset = false, onlyBlocking = false, onPause = {}, onFilter = {}, onStop = {})
            }
        }
    }
    @Test fun livePausedNoHeadset() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp)) {
                it.pixelbox.cmwatch.mobile.ui.LivePill(null, speaking = false, paused = true, noHeadset = true, onlyBlocking = true, onPause = {}, onFilter = {}, onStop = {})
            }
        }
    }

    // La pillola della live aperta (B2, Franz 08/10 21:47): una riga per sessione, filtro, tasti.
    @Test fun liveOpen() = paparazzi.snapshot {
        val st = it.pixelbox.cmwatch.contract.ContractJson.decodeState(java.io.File("../contract/state-1-question.json").readText())
        CmPhoneTheme(still = true) {
            Box(Modifier.background(CmColors.bg).padding(12.dp)) {
                it.pixelbox.cmwatch.mobile.ui.LivePill("ledger-api chiede: deploy now?", speaking = true, paused = false, noHeadset = false, onlyBlocking = false, onPause = {}, onFilter = {}, onStop = {},
                    rows = it.pixelbox.cmwatch.rules.LivePanel.rows(st, 1_789_210_700L + 12 * 60), startOpen = true)
            }
        }
    }
}
