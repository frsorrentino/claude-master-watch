package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

class LaunchSheetTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())

    @Test fun launchSheet() = paparazzi.snapshot { CmPhoneTheme(still = true) { LaunchSheet(st) { _, _ -> } } }

    // Contratto 1.49 (Franz, 09/10 21:27, scelta A): nel foglio della notte le schede del Recap da spuntare, prima quelle
    // che può fare Claude; le tue chiuse sotto la loro riga. Due spuntate: il tasto tonale «Metti stanotte · 2».
    @Test fun nightSheetWithRecapCards() = paparazzi.snapshot {
        val rows = it.pixelbox.cmwatch.rules.RecapAgenda.night(it.pixelbox.cmwatch.contract.AgendaPage(listOf(
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "agenzia", "franz", "Confirm the 6 client ids with a candidate", ".claude/to-decide-client-id.md", key = "k1"),
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "personale", "claude", "Move the docs site to the new host", "orbit-docs", key = "k2"),
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "postazione", "claude", "Clear the orphan plugin caches", "after restarting every session", key = "k3"),
            it.pixelbox.cmwatch.contract.AgendaRow("aperto", "agenzia", "terzi", "Client answer on the domain", "", key = "k4"),
        )))
        CmPhoneTheme(still = true) {
            LaunchSheet(st, action = R.string.night_add, top = { NightAgendaPicker(rows, initial = listOf("k2", "k3")) {} }) { _, _ -> }
        }
    }
}
