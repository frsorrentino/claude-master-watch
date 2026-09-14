package it.pixelbox.cmwatch.wear

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRound
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.wear.ui.screens.OutcomeScreen
import it.pixelbox.cmwatch.wear.ui.screens.PairingScreen
import it.pixelbox.cmwatch.wear.ui.screens.TimelineScreen
import it.pixelbox.cmwatch.wear.ui.screens.PairingStatus
import it.pixelbox.cmwatch.wear.ui.screens.QuestionScreen
import it.pixelbox.cmwatch.wear.ui.screens.QuotaScreen
import it.pixelbox.cmwatch.wear.ui.screens.SessionScreen
import it.pixelbox.cmwatch.wear.ui.screens.SessionsScreen
import it.pixelbox.cmwatch.wear.ui.theme.CmTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Pixel Watch 5 da 45 mm: 456×456, tondo. Si registra in GitHub Actions (x86_64): `./gradlew :wear:recordPaparazziDebug`. */
class ScreensSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        // In inglese, per le card del README pubblico (Franz, 14/09 22:39); sul polso l'app resta in italiano.
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND.copy(screenWidth = 456, screenHeight = 456, density = Density.XHIGH, screenRound = ScreenRound.ROUND, locale = "en"),
        theme = "android:Theme.DeviceDefault.NoActionBar",
    )
    private val now = 1789210800L
    private val state = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val snap = Snapshot(state, Freshness.Fresh)

    @Test fun sessions() = paparazzi.snapshot { CmTheme { SessionsScreen(snap, now, onOpen = {}, onSettings = {}) } }
    @Test fun card() = paparazzi.snapshot { CmTheme { SessionScreen(snap, "atlas-shop", now, {}, {}, {}, {}, {}, {}) } }
    @Test fun question() = paparazzi.snapshot { CmTheme { QuestionScreen(snap, "ledger-api", now, null, {}, {}, {}, {}, {}, {}) } }
    @Test fun stale() = paparazzi.snapshot { CmTheme { SessionsScreen(snap.copy(freshness = Freshness.Stale(12)), now, onOpen = {}, onSettings = {}) } }
    @Test fun pairing() = paparazzi.snapshot { CmTheme { PairingScreen(PairingStatus.Idle, {}, {}) } }
    // Per le card del README (Franz, 14/09 22:47): i due account della fixture, uno fresco e uno con il dato vecchio.
    @Test fun quota() = paparazzi.snapshot { CmTheme { QuotaScreen(state, Freshness.Fresh, now) } }
    // S07: Esito e Timeline, le due schermate dove il bordo tondo tagliava le lettere.
    @Test fun outcome() = paparazzi.snapshot { CmTheme { OutcomeScreen(snap, "atlas-shop", now, 120, false, {}, {}, {}) } }
    @Test fun timeline() = paparazzi.snapshot { CmTheme { TimelineScreen(ContractJson.decodeEvents(File("../contract/events-sample.json").readText())) } }
}

/** S07: le stesse schermate con i caratteri di sistema al massimo di Wear OS (1,24): il testo cresce, non si taglia. */
class LargeFontSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND.copy(screenWidth = 456, screenHeight = 456, density = Density.XHIGH, screenRound = ScreenRound.ROUND, locale = "en", fontScale = 1.24f),
        theme = "android:Theme.DeviceDefault.NoActionBar",
    )
    private val now = 1789210800L
    private val state = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val snap = Snapshot(state, Freshness.Fresh)

    @Test fun sessionsLargeFont() = paparazzi.snapshot { CmTheme { SessionsScreen(snap, now, onOpen = {}, onSettings = {}) } }
    @Test fun cardLargeFont() = paparazzi.snapshot { CmTheme { SessionScreen(snap, "atlas-shop", now, {}, {}, {}, {}, {}, {}) } }
    @Test fun questionLargeFont() = paparazzi.snapshot { CmTheme { QuestionScreen(snap, "ledger-api", now, null, {}, {}, {}, {}, {}, {}) } }
    @Test fun outcomeLargeFont() = paparazzi.snapshot { CmTheme { OutcomeScreen(snap, "atlas-shop", now, 120, false, {}, {}, {}) } }
}
