package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

class SessionSheetTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

    @Test fun sheetQuestion() = paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(st.sessions.first { it.question != null }, st.ts, emptyList(), 120, none) } }
    @Test fun sheetIdleWithOutcome() = paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(st.sessions.first { it.state == SessionState.IDLE }, st.ts, emptyList(), 120, none) } }
    @Test fun sheetClosed() = paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(st.sessions.first().copy(state = SessionState.GONE, question = null), st.ts, emptyList(), 120, none) } }
    @Test fun sheetHighTier() = paparazzi.snapshot {
        val s = st.sessions.first { it.question != null }
        CmPhoneTheme(still = true) { SessionSheet(s.copy(question = s.question!!.copy(tier = Tier.HIGH)), st.ts, emptyList(), 120, none) }
    }
    @Test fun sheetBusyWithGoal() = paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(st.sessions.first { it.goal != null }, st.ts, emptyList(), 120, none) } }
}
