package it.pixelbox.cmwatch.wear

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.ScreenRound
import it.pixelbox.cmwatch.rules.LiveCard
import it.pixelbox.cmwatch.rules.LivePanel
import it.pixelbox.cmwatch.wear.ui.screens.LiveScreen
import it.pixelbox.cmwatch.wear.ui.theme.CmTheme
import org.junit.Rule
import org.junit.Test

/** Le schede della modalità live (specifica 06/10, §9): domanda, esito con Prossimi, doppia conferma. */
class LiveScreenSnapshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.WEAR_OS_SMALL_ROUND.copy(screenWidth = 456, screenHeight = 456, density = Density.XHIGH, screenRound = ScreenRound.ROUND, locale = "en"),
        theme = "android:Theme.DeviceDefault.NoActionBar",
    )
    private val now = 1_789_220_000_000L

    @Test fun liveQuestion() = paparazzi.snapshot {
        CmTheme { LiveScreen(LiveCard(1, LiveCard.Kind.QUESTION, "ledger api", "Deploy ready, waiting for the client's ok. Deploy now?", listOf("yes", "no")), now, {}, {}) }
    }

    @Test fun liveOutcome() = paparazzi.snapshot {
        CmTheme {
            LiveScreen(
                LiveCard(2, LiveCard.Kind.OUTCOME, "atlas shop", "Migrations 008-011 applied, tests green", listOf("ok to deploy on staging", "review the test seeds"), listOf(true, false)),
                now, {}, {},
            )
        }
    }

    @Test fun liveConfirm() = paparazzi.snapshot {
        CmTheme {
            LiveScreen(
                LiveCard(3, LiveCard.Kind.CONFIRM, "Release 2.4 of atlas-shop", "tag v2.4 and push to origin main, on production (shop.example.com)", until = now + 7_000),
                now, {}, {},
            )
        }
    }

    // La live tranquilla con le sessioni, come la pillola aperta del telefono (Franz, 10/10 10:50): chi chiede, chi lavora,
    // chi è ferma, con il pallino del colore del suo stato.
    @Test fun liveIdleRows() = paparazzi.snapshot {
        CmTheme {
            LiveScreen(
                LiveCard(
                    1, LiveCard.Kind.IDLE,
                    rows = listOf(
                        LivePanel.Row("ledger-api", LivePanel.Kind.ASKING, "Deploy now?", at = 1_789_219_700),
                        LivePanel.Row("atlas-shop", LivePanel.Kind.WORKING, "Bash pytest -q tests", minutes = 6),
                        LivePanel.Row("field-notes", LivePanel.Kind.STILL, "README rewritten with the three sections", at = 1_789_219_000),
                    ),
                ),
                now, {}, {},
            )
        }
    }
}
