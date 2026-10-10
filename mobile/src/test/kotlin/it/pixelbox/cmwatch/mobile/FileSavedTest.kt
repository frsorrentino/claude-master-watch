package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.ui.tokens.CmColors
import org.junit.Rule
import org.junit.Test

/** La conferma di «Scarica» (Franz, 10/10 06:50: «servirebbe conferma download e opzioni apri cartella o apri file»). */
class FileSavedTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private fun saved(inDownloads: Boolean) = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            Column(Modifier.background(CmColors.surface).padding(top = 24.dp)) {
                FileSavedContent(
                    SavedFile("release-notes-2.4.0-rc1.pdf", "content://media/external/downloads/1", "application/pdf", "/cache/files/x", inDownloads),
                    onOpen = {}, onFolder = if (inDownloads) ({}) else null, onElsewhere = if (inDownloads) ({}) else null,
                )
            }
        }
    }

    // In Download: «Apri» pieno, poi «Apri la cartella» e «Salva altrove».
    @Test fun savedInDownloads() = saved(inDownloads = true)
    // Nella cartella scelta col foglio di sistema: solo «Apri».
    @Test fun savedElsewhere() = saved(inDownloads = false)
}
