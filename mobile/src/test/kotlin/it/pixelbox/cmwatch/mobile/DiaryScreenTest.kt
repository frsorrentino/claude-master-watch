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
        night = Night(queued = 2, running = null),
    )
    private val quota = listOf(Event("q1", EventKind.QUOTA, account = "personale", ts = st.ts, title = "personale al 95%", body = "si azzera alle 18:40"))

    @Test fun diaryFull() = paparazzi.snapshot { CmPhoneTheme { DiaryScreen(st, quota, 120, {}) } }
    @Test fun diaryEmpty() = paparazzi.snapshot { CmPhoneTheme { DiaryScreen(st.copy(recap = Recap(), night = Night()), emptyList(), 120, {}) } }
}
