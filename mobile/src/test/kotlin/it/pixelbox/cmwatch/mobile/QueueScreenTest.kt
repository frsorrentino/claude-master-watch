package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

/** La coda «Ti aspettano»: due domande in fila (la seconda chiesta più tardi, rischio alto) e la coda vuota. */
class QueueScreenTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})
    private val q = st.sessions.first { it.question != null }
    private val two = st.copy(sessions = st.sessions + q.copy(
        id = "s-second", name = "atlas-review", state = SessionState.WAITING,
        question = q.question!!.copy(id = "q-second", tier = Tier.HIGH, askedAt = q.question!!.askedAt + 60),
    ))

    @Test fun queueTwo() = paparazzi.snapshot { CmPhoneTheme(still = true) { QueueScreen(two, st.ts + 300, { none }, {}) } }

    @Test fun queueEmpty() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { QueueScreen(st.copy(sessions = st.sessions.map { it.copy(question = null) }), st.ts, { none }, {}) }
    }
}
