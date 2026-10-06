package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.TunePanel
import it.pixelbox.cmwatch.rules.MasterService
import org.junit.Rule
import org.junit.Test

/** Il pannello Modello ed effort (bozza approvata da Franz il 07/10, docs/mockup/2026-10-06-modello-effort/). */
class TunePanelTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")

    private val st = ContractJson.decodeState(java.io.File("../contract/state-1-question.json").readText())
    private val atlas = st.sessions.first { it.name == "atlas-shop" }
    private val choices = st.choices!!

    // Bozza 1: Sonnet 5 · medium, consigliati Fable 5.1 e high a metà lavoro: costo e «Usa il consiglio».
    @Test fun adviceDiffersMidWork() = paparazzi.snapshot {
        val a = MasterService.advice(atlas, choices, atlas.advice!!.at)
        CmPhoneTheme(still = true) { TunePanel(choices, atlas.model?.id, atlas.effort, a, {}, {}) {} }
    }

    // Bozza 2: la scelta è già quella consigliata: «Va bene così», senza tasto e senza costo.
    @Test fun adviceSameAsTheChoice() = paparazzi.snapshot {
        val same = atlas.copy(advice = atlas.advice!!.copy(model = "claude-sonnet-5", effort = "medium", differs = false))
        val a = MasterService.advice(same, choices, atlas.advice!!.at)
        CmPhoneTheme(still = true) { TunePanel(choices, atlas.model?.id, atlas.effort, a, {}, {}) {} }
    }

    // Senza consiglio: solo modelli ed effort.
    @Test fun noAdvice() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { TunePanel(choices, atlas.model?.id, atlas.effort, null, {}, {}) {} }
    }
}
