package it.pixelbox.cmwatch.mobile

import androidx.compose.runtime.Composable
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

    @Composable
    private fun board(inspector: Boolean = true) = CmPhoneTheme(still = true) {
        val summary = Summary.build(st, emptyList(), emptyList(), ts, zone, emptySet())
        val groups = Tablet.groups(summary)
        val ring = PhoneOverview.build(st, emptyList(), samples, ts, zone, stale = false).rings.first { it.account == selected.account }
        val row = groups.flatMap { it.second }.first { it.session.name == selected.name }
        TabletShell(
            Tablet.status(st, summary, ts, stale = false), watch = true, view = TabletView.BOARD, onView = {}, registerOpen = false,
            rail = RailActions({}, {}, {}, {}, {}), now = ts,
            sessions = {
                TabletSessions(groups, st.sessions.count { it.state != SessionState.GONE }, summary.closed.size, selected.name, ts, onPick = {}, bottom = { TabletQuotaPanel(ring, ts) })
            },
            center = {
                SessionSheet(
                    selected, ts, emptyList(), 120, none, choices = st.choices, ops = st.ops, canAttach = true, feed = feed,
                    accountQuota = st.quota[selected.account], headerLead = { TabletConversationLead(row, selected, ts) }, notesInHeader = !inspector,
                )
            },
            inspector = if (inspector) ({ TabletInspector(Tablet.inspect(selected, timeline, ts, zone), st.quota[selected.account]?.h5, loading = false) }) else null,
        )
    }

    // La plancia (mockup «0-panoramica», sezioni 1-3): riga di stato, barra, sessioni e quote, conversazione, ispettore.
    @Test fun tabletBoard() = paparazzi.snapshot { board() }

    // Carattere grande (standard della master, 04/10): la riga di stato resta una riga e lascia fuori i pezzi meno importanti.
    @Test fun tabletBoardLargeFont() {
        paparazzi.unsafeUpdateConfig(deviceConfig = TABLET.copy(fontScale = 1.5f))
        paparazzi.snapshot { board() }
    }

    // Una finestra da 1000 dp (Chromebook): niente ispettore, la conversazione prende il posto.
    @Test fun tabletBoardNarrow() {
        paparazzi.unsafeUpdateConfig(deviceConfig = TABLET.copy(screenWidth = 2000, screenHeight = 1400))
        paparazzi.snapshot { board(inspector = false) }
    }

    private fun shortFeed(name: String) = ChatFeed.merge(
        when (name) {
            "master" -> listOf(
                TranscriptEntry("m1", "user", text = "I'd like to see the tablet version of the app", at = ts - 40 * 60, origin = "phone"),
                TranscriptEntry("m2", "assistant", text = "Tablet plan written and handed to atlas-shop: board first, then the columns.", at = ts - 18 * 60),
            )
            "field-notes" -> listOf(
                TranscriptEntry("f1", "user", text = "Rewrite the README with the three sections", at = ts - 3 * 3600, origin = "pc"),
                TranscriptEntry("f2", "assistant", text = "README rewritten with the three sections asked for.\n\nEsito: README rewritten", at = ts - 3 * 3600 + 300),
            )
            else -> listOf(
                TranscriptEntry("l1", "user", text = "Prepare the deploy of 2.4 and wait for my ok", at = ts - 20 * 60, origin = "pc"),
                TranscriptEntry("l2", "assistant", text = "The build is ready and the migration notes are checked. I'm waiting for the client's ok before the deploy.", at = ts - 6 * 60),
            )
        },
        emptyList(),
    )

    @Composable
    private fun columns(cols: List<String>, barOpen: Boolean, fixed: Boolean) = CmPhoneTheme(still = true) {
        val summary = Summary.build(st, emptyList(), emptyList(), ts, zone, emptySet())
        val groups = Tablet.groups(summary)
        val ring = PhoneOverview.build(st, emptyList(), samples, ts, zone, stale = false).rings.first { it.account == selected.account }
        TabletColumns(
            Tablet.status(st, summary, ts, stale = false), ring, ts, groups, st.sessions.count { it.state != SessionState.GONE },
            columns = cols, onToggle = {}, onBoard = {}, barFixed = fixed, barOpen = barOpen, onBar = {}, onBarFixed = {},
        ) { r ->
            SessionSheet(
                r.session, ts, emptyList(), 120, none, ops = st.ops, canAttach = true, header = false,
                feed = if (r.session.name == selected.name) feed else shortFeed(r.session.name),
                appBar = { TabletColumnHeader(r, ts, onClose = {}) },
            )
        }
    }

    // Le colonne con la barra fissa (mockup 4-5): tre sessioni, chi ti aspetta col bordo arancio.
    @Test fun tabletColumnsBarFixed() = paparazzi.snapshot { columns(listOf("atlas-shop", "ledger-api", "master"), barOpen = true, fixed = true) }

    // Barra richiudibile e chiusa (mockup 6-7): la striscia di puntini, quattro colonne.
    @Test fun tabletColumnsBarClosed() = paparazzi.snapshot { columns(listOf("atlas-shop", "ledger-api", "master", "field-notes"), barOpen = false, fixed = false) }

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
