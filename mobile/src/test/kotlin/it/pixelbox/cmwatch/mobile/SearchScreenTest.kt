package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.Sent
import org.junit.Rule
import org.junit.Test

/** La ricerca con un testo scritto: risultati da un messaggio, da un esito e dal diario, la parte trovata in grassetto. */
class SearchScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val t = 1789210800L
    private val sent = listOf(
        Sent("a", "ledger-api", "Perché il deploy fallisce in staging?", t - 7200, outcomeFull = "Il deploy ora passa: mancava una variabile.", doneAt = t - 7000),
    )
    private val events = listOf(Event("e", EventKind.RECAP, ts = t - 600, title = "Diario", body = "Oggi: deploy di atlas-shop e test di ledger-api"))

    @Test fun searchResults() = paparazzi.snapshot { CmPhoneTheme(still = true) { SearchScreen(sent, events, {}, initial = "deploy") } }
}
