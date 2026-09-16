package it.pixelbox.cmwatch.wear

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRound
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.wear.ui.screens.PairingScreen
import it.pixelbox.cmwatch.wear.ui.screens.TimelineScreen
import it.pixelbox.cmwatch.wear.ui.screens.TerminalScreen
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FailureConfirmationDialogContent
import androidx.wear.compose.material3.SuccessConfirmationDialogContent
import androidx.wear.compose.material3.confirmationDialogCurvedText
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
    @Test fun card() = paparazzi.snapshot { CmTheme { SessionScreen(snap, "atlas-shop", now, {}, {}, {}, {}, {}) } }
    @Test fun question() = paparazzi.snapshot { CmTheme { QuestionScreen(snap, "ledger-api", now, null, {}, {}, {}, {}, {}, {}) } }
    @Test fun stale() = paparazzi.snapshot { CmTheme { SessionsScreen(snap.copy(freshness = Freshness.Stale(12)), now, onOpen = {}, onSettings = {}) } }
    @Test fun pairing() = paparazzi.snapshot { CmTheme { PairingScreen(PairingStatus.Idle, {}, {}) } }
    // Per le card del README (Franz, 14/09 22:47): i due account della fixture, uno fresco e uno con il dato vecchio.
    @Test fun quota() = paparazzi.snapshot { CmTheme { QuotaScreen(state, Freshness.Fresh, now) } }
    // S07: la Scheda di una sessione ferma, con l'esito intero (era la schermata Esito), e la Timeline.
    @Test fun cardIdle() = paparazzi.snapshot { CmTheme { SessionScreen(snap, "field-notes", now, {}, {}, {}, {}, {}) } }
    @Test fun timeline() = paparazzi.snapshot { CmTheme { TimelineScreen(ContractJson.decodeEvents(File("../contract/events-sample.json").readText())) } }
    // Design 15/09: il Terminale come un copione — il prompt sul filo azzurro, la prosa di Claude, strumento e output in mono.
    @Test fun terminal() = paparazzi.snapshot {
        CmTheme { TerminalScreen("atlas-shop", terminalSample, loading = false, error = null, answer = emptyList(), capturedAt = now * 1000) }
    }
    // Le due conferme del «segui», una accanto all'altra: coppia simmetrica e segno dominante (Franz, 16/09 02:44).
    @Test fun confirmFollow() = paparazzi.snapshot {
        CmTheme {
            // Lo stile curvo si legge qui: il getter è @Composable, la lambda del testo curvo no.
            val curvo = ConfirmationDialogDefaults.curvedTextStyle
            SuccessConfirmationDialogContent(curvedText = { confirmationDialogCurvedText("Alerts on", curvo) })
        }
    }

    @Test fun confirmUnfollow() = paparazzi.snapshot {
        CmTheme {
            val curvo = ConfirmationDialogDefaults.curvedTextStyle
            FailureConfirmationDialogContent(curvedText = { confirmationDialogCurvedText("Alerts off", curvo) })
        }
    }

    private val terminalSample = listOf(
        "❯ Run the tests, then update", "the changelog", "⏺ Running the suite.",
        "⏺ Bash(pytest -q)", "⎿ 42 passed in 3.1s", "⏺ All green, moving to the", "changelog.",
    ).joinToString("\n")
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
    @Test fun cardLargeFont() = paparazzi.snapshot { CmTheme { SessionScreen(snap, "atlas-shop", now, {}, {}, {}, {}, {}) } }
    @Test fun questionLargeFont() = paparazzi.snapshot { CmTheme { QuestionScreen(snap, "ledger-api", now, null, {}, {}, {}, {}, {}, {}) } }
    @Test fun cardIdleLargeFont() = paparazzi.snapshot { CmTheme { SessionScreen(snap, "field-notes", now, {}, {}, {}, {}, {}) } }
}
