package it.pixelbox.cmwatch.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.resources.Density
import com.android.resources.Keyboard
import com.android.resources.KeyboardState
import com.android.resources.ScreenOrientation
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.rules.ChatFeed
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.QuotaHistory
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.rules.Tablet
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId

/**
 * Il tablet (piano 04/10, mockup in docs/mockup/2026-10-04-tablet/): la plancia a 1440×900 dp in orizzontale, la stessa con
 * il carattere grande, e una finestra da 1000 dp senza ispettore. Solo i dati demo della fixture: il repo è pubblico.
 */
class TabletShellTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = TABLET, theme = "android:Theme.Material.NoActionBar")

    private val base = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val ts = base.ts
    private val zone = ZoneId.systemDefault()
    private val master = base.sessions.first { it.state == SessionState.IDLE }.copy(
        id = "demo-master", name = "master", project = "personal/master", question = null, context = 31, since = ts - 18 * 60,
        outcome = Outcome("Tablet plan written", "Tablet plan written and handed to atlas-shop", ts - 18 * 60),
    )
    private val st = base.copy(
        sessions = base.sessions.map {
            if (it.name == "atlas-shop") it.copy(tool = "Bash", toolNote = "Run the checkout tests", context = 61, turnStarted = ts - 6 * 60, since = ts - (5 * 3600 + 12 * 60)) else it
        } + master,
        quota = base.quota + ("personal" to base.quota.getValue("personal").copy(h5 = 9, w7 = 24, resetH5 = ts + 2 * 3600 + 40 * 60)),
    )
    private val selected = st.sessions.first { it.name == "atlas-shop" }
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

    private val samples = mapOf(
        "personal" to listOf(
            QuotaHistory.Sample(ts - 2 * 3600, 2), QuotaHistory.Sample(ts - 3600, 4), QuotaHistory.Sample(ts - 1800, 6), QuotaHistory.Sample(ts, 9),
        ),
    )

    private val timeline = TimelinePage(
        since = ts - 6 * 3600,
        sessions = listOf(
            TimelineSession("atlas-shop", live = true, project = "personal/atlas-shop", events = listOf(
                TimelineEvent(ts - 110 * 60, "commit", "fix the cart totals", ref = "4be1c2a"),
                TimelineEvent(ts - 95 * 60, "prompt", "ok, run the checkout tests", ref = "phone"),
                TimelineEvent(ts - 80 * 60, "test", "checkout suite", ok = true, ref = "48 passed"),
                TimelineEvent(ts - 62 * 60, "commit", "tag the release candidate", ref = "91d0e7f"),
                TimelineEvent(ts - 40 * 60, "test", "payment suite", ok = false, ref = "2 failed"),
                TimelineEvent(ts - 25 * 60, "outcome", "release candidate tagged, payments to fix"),
                TimelineEvent(ts - 8 * 60, "prompt", "yes, fix the payments", ref = "phone"),
            )),
        ),
    )

    private val feed = ChatFeed.merge(
        listOf(
            TranscriptEntry("u1.0", "user", text = "Tag the release candidate and run the checkout tests", at = ts - 70 * 60, origin = "phone"),
            TranscriptEntry("a1.0", "assistant", text = "I'll run the checkout suite first.", at = ts - 69 * 60),
            TranscriptEntry("a1.1", "tool", text = "./gradlew checkoutTest", at = ts - 68 * 60, tool = "Bash", note = "Run the checkout tests"),
            TranscriptEntry("a1.2", "assistant", text = "Checkout suite green: 48 passed. The release candidate is tagged as 2.4.0-rc1; the payment suite still has two failures in the refund path.\n\nEsito: release candidate tagged, payments to fix", at = ts - 25 * 60, turn = TranscriptTurn(ts - 70 * 60, ts - 25 * 60, 52_000, 1_900)),
            TranscriptEntry("u2.0", "user", text = "yes, fix the payments", at = ts - 8 * 60, origin = "phone"),
            TranscriptEntry("a2.0", "assistant", text = "Looking at the refund path now.", at = ts - 7 * 60),
            TranscriptEntry("a2.1", "tool", text = "./gradlew paymentTest", at = ts - 6 * 60, tool = "Bash", note = "Run the checkout tests"),
        ),
        emptyList(),
    )

    private fun shortFeed(name: String) = ChatFeed.merge(
        when (name) {
            "master" -> listOf(
                TranscriptEntry("m1", "user", text = "I'd like to see the tablet version of the app", at = ts - 40 * 60, origin = "phone"),
                TranscriptEntry("m2", "assistant", text = "Tablet plan written and handed to atlas-shop: board first, then the columns.", at = ts - 18 * 60),
            )
            else -> listOf(
                TranscriptEntry("l1", "user", text = "Prepare the deploy of 2.4 and wait for my ok", at = ts - 20 * 60, origin = "pc"),
                TranscriptEntry("l2", "assistant", text = "The build is ready and the migration notes are checked. I'm waiting for the client's ok before the deploy.", at = ts - 6 * 60),
            )
        },
        emptyList(),
    )

    /** La vista unica (Franz, 04/10 16:36): la home del telefono di lato, le colonne, i dettagli se accesi. */
    @Composable
    private fun desk(cols: List<String>, shares: List<Int>, homeRight: Boolean = false, details: Boolean = false) = CmPhoneTheme(still = true) {
        val summary = Summary.build(st, emptyList(), emptyList(), ts, zone, emptySet())
        val rows = Tablet.groups(summary).flatMap { it.second }
        val rings = PhoneOverview.build(st, emptyList(), samples, ts, zone, stale = false).rings
        TabletDesk(
            home = {
                SummaryList(summary, {}, { _, _ -> }, { _, _ -> }, {}, {}, footer = {
                    androidx.compose.foundation.layout.Column(
                        androidx.compose.ui.Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                    ) { rings.forEach { TabletQuotaPanel(it, ts) } }
                })
            },
            homeRight = homeRight, onHomeSide = {}, columns = cols, shares = shares, onSwap = { _, _ -> }, onShares = {},
            column = { name, drag ->
                val r = rows.first { it.session.name == name }
                SessionSheet(
                    r.session, ts, emptyList(), 120, none, ops = st.ops, canAttach = true, header = true, choices = st.choices,
                    feed = if (name == selected.name) feed else shortFeed(name),
                    appBar = { TabletColumnHeader(r, ts, onClose = {}, drag = drag) },
                )
            },
            empty = { androidx.compose.material3.Text("Tocca una sessione nella home per aprirla qui; ne stanno fino a quattro, affiancate.") },
            inspector = if (details) ({ TabletInspector(Tablet.inspect(selected, timeline, ts, zone), st.quota[selected.account]?.h5, loading = false) }) else null,
        )
    }

    // La home a sinistra e due colonne, la prima larga due terzi (8 dodicesimi).
    @Test fun tabletDesk() = paparazzi.snapshot { desk(listOf("atlas-shop", "ledger-api"), listOf(8, 4)) }

    // La home a destra (un clic su ⇄), i dettagli dall'altro lato, due colonne uguali.
    @Test fun tabletDeskHomeRightDetails() = paparazzi.snapshot { desk(listOf("atlas-shop", "ledger-api"), listOf(6, 6), homeRight = true, details = true) }

    // Carattere grande, tre colonne uguali.
    @Test fun tabletDeskLargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = TABLET.copy(fontScale = 1.5f))
        paparazzi.snapshot { desk(listOf("atlas-shop", "ledger-api", "master"), listOf(4, 4, 4)) }
    }

    // Nessuna colonna ancora: l'invito a toccare una sessione nella home.
    @Test fun tabletDeskEmpty() = paparazzi.snapshot { desk(emptyList(), emptyList()) }

    // La barra del titolo nostra sul Pixel Tablet in finestra (Franz, 05/10 11:21): schede delle colonne, quota a destra, i
    // tasti di sistema lasciati liberi (qui 140 dp a destra, come quelli di Android).
    @Test fun captionBar() = paparazzi.snapshot {
        CmPhoneTheme(still = true) {
            androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.background(it.pixelbox.cmwatch.ui.tokens.CmColors.bg)) {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxWidth().height(40.dp)) {
                    it.pixelbox.cmwatch.mobile.ui.CaptionStrip(androidx.compose.ui.Modifier.padding(end = 140.dp)) {
                        st.sessions.take(2).forEach { ses -> it.pixelbox.cmwatch.mobile.ui.CaptionTab(ses, onFocus = {}, onClose = {}) }
                        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.weight(1f))
                        androidx.compose.material3.Text("personale · 5h 11%", style = it.pixelbox.cmwatch.mobile.ui.MonoSmall)
                    }
                }
            }
        }
    }

    companion object {
        /** Tablet in orizzontale, 1440×900 dp. */
        val TABLET = DeviceConfig.PIXEL_5.copy(
            screenWidth = 2880, screenHeight = 1800, xdpi = 320, ydpi = 320, density = Density.XHIGH,
            orientation = ScreenOrientation.LANDSCAPE, locale = "it",
            // Con la tastiera fisica, come il Chromebook: il campo dice «Invio manda».
            keyboard = Keyboard.QWERTY, keyboardState = KeyboardState.EXPOSED,
        )
    }
}
