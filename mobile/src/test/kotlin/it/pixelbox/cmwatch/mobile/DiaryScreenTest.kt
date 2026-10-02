package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

class DiaryScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-2-idle.json").readText()).copy(
        recap = Recap("2026-09-29", listOf(RecapItem("kb", "Distillata l'analisi Play", "Rivedere l'indice"), RecapItem("watch", "Specifica del pezzo 3"))),
        night = Night(queued = 2, running = null, items = listOf(
            NightItem("a3f09c1e", "/w/atlas-shop", "atlas-shop", "Go through the open issues labelled flaky and fix the real ones", 1789207200, started = 1789236000),
            NightItem("7b21d4e8", "/w/ledger-api", "ledger-api", "Update the changelog for 2.4 and check the migration notes", 1789210620),
        )),
    )
    private val history = listOf(
        Event("r1", EventKind.RECAP, ts = st.ts - 86400, title = "Diario del 28/09", body = "Diario 28/09/2026 · 2 progetti\n\nCHIUSI OGGI\n✓ kb · 3 turni\n   Indice rivisto\n✓ watch · 5 turni\n   Piano A scritto", ref = "2026-09-28"),
    )
    private val night = Event("n1", EventKind.NIGHT_REPORT, ts = st.ts - 3600, title = "Notte: 2 lavori, 1 riuscito", body = "Notte: 2 lavori, 0 ancora in coda\n✓ atlas-shop (personale, 812 s, rc=0): Tre test instabili sistemati, due erano veri.\n   /w/atlas-shop/docs/notte/a3f09c1e.md\n✗ ledger-api (lavoro, 3600 s, rc=124): tempo scaduto\n   /w/ledger-api/docs/notte/7b21d4e8.md", ref = "2026-09-29")
    private val quota = listOf(Event("q1", EventKind.QUOTA, account = "personale", ts = st.ts, title = "personale al 95%", body = "si azzera alle 18:40"))

    // Il Registro (mockup approvato da Franz, 02/10 09:10): Stanotte, Notte a righe, Oggi aperto, Ieri chiuso, quota.
    private val rings = listOf(
        it.pixelbox.cmwatch.rules.PhoneOverview.Ring("personale", true, 42, 18, null, null),
        it.pixelbox.cmwatch.rules.PhoneOverview.Ring("lavoro", false, 7, 63, null, null),
    )
    @Test fun diaryFull() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { DiaryScreen(st, quota, history, night, 120, {}, onAdd = {}, onRemove = {}, rings = rings, today = java.time.LocalDate.of(2026, 9, 29)) }
    }
    @Test fun diaryEmpty() = paparazzi.snapshot { CmPhoneTheme(still = true) { DiaryScreen(st.copy(recap = Recap(), night = Night(items = emptyList())), emptyList(), emptyList(), null, 120, {}, onAdd = {}, onRemove = {}) } }
    @Test fun diaryOldRelay() = paparazzi.snapshot { CmPhoneTheme(still = true) { DiaryScreen(st.copy(night = Night(queued = 1)), emptyList(), emptyList(), null, 120, {}, onAdd = {}, onRemove = {}) } }
}
