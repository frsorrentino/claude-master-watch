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

    // Contratto 1.27: i risultati del relay nelle conversazioni di tutte le sessioni, con una sessione chiusa.
    @Test fun searchConversations() = paparazzi.snapshot {
        val page = it.pixelbox.cmwatch.contract.SearchPage(listOf(
            it.pixelbox.cmwatch.contract.SearchHit("atlas-shop", true, entry = "a1.0", role = "assistant", at = t - 300, snippet = "The review of the migration scripts moved to Tuesday after the client asked for one more pass.", match = listOf(45, 52)),
            it.pixelbox.cmwatch.contract.SearchHit("orbit-docs", false, entry = "c5.0", role = "user", at = t - 5400, snippet = "move the review to tuesday", match = listOf(19, 26)),
            it.pixelbox.cmwatch.contract.SearchHit("orbit-docs", false, entry = "c1.0", role = "user", at = t - 7200, snippet = "Perché la riunione di Tuesday è saltata?", match = listOf(22, 29)),
        ), more = true)
        CmPhoneTheme(still = true) { SearchScreen(emptyList(), events, {}, initial = "tuesday", remote = true, page = page, known = setOf("atlas-shop")) }
    }
}
