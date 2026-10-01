package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.rules.WidgetModel.Metric
import it.pixelbox.cmwatch.rules.WidgetModel.Mode
import org.junit.Assert.*
import org.junit.Test

/**
 * Il widget della schermata home (piano 30/09, Task 7), nello stile di ads-widget: una card con l'arco a sinistra e le
 * colonne scelte. Fixture: ledger-api (lavoro, seguita, ctx 62) aspetta una risposta, atlas-shop (personale, ctx 18)
 * lavora, field-notes (personale, ctx 4) è ferma, orbit-docs è sparita; personale h5 11 %, settimana 36 %.
 */
class WidgetModelTest {
    private val state = ContractJson.decodeState(Fixtures.stateQuestion)
    private val now = state.ts
    private fun cfg(mode: Mode, target: String? = null, metrics: List<Metric> = WidgetModel.defaults(mode)) =
        WidgetModel.Config(mode, target, metrics, opacity = 85, corners = 24, mono = false)

    @Test fun accountArcIsTheFiveHourQuota() {
        val c = WidgetModel.cards(state, cfg(Mode.ACCOUNT, "personal", listOf(Metric.WEEK, Metric.WORKING, Metric.IDLE)), now).single()
        assertEquals("personal", c.title)
        assertEquals(11, c.arcPct); assertEquals(WidgetModel.ARC_5H, c.arcLabel)
        // Solo le sessioni dell'account: atlas-shop lavora, field-notes è ferma.
        assertEquals(listOf(Metric.WEEK to "36%", Metric.WORKING to "1", Metric.IDLE to "1"), c.columns)
        assertEquals(state.ts, c.updatedAt)
    }

    /** Un dato vecchio non disegna l'arco: il lavoro ha h5 assente e stale. */
    @Test fun staleAccountHasNoArc() = assertNull(WidgetModel.cards(state, cfg(Mode.ACCOUNT, "work"), now).single().arcPct)

    @Test fun sessionArcIsTheContext() {
        val c = WidgetModel.cards(state, cfg(Mode.SESSION, "atlas-shop"), now).single()
        assertEquals("atlas-shop", c.title); assertEquals(18, c.arcPct); assertEquals(WidgetModel.ARC_CTX, c.arcLabel)
        assertEquals("atlas-shop", c.session)
        // Senza scelta: la sessione seguita.
        assertEquals("ledger-api", WidgetModel.cards(state, cfg(Mode.SESSION, null), now).single().title)
    }

    @Test fun boardCountsWaitingWorkingIdle() {
        val c = WidgetModel.cards(state, cfg(Mode.BOARD, metrics = listOf(Metric.WAITING, Metric.WORKING, Metric.IDLE)), now).first()
        assertEquals(listOf(Metric.WAITING to "1", Metric.WORKING to "1", Metric.IDLE to "1"), c.columns)
        assertEquals(11, c.arcPct)
        assertNull(c.session)
    }

    @Test fun widgetWithoutStateSaysWaiting() {
        val c = WidgetModel.cards(null, cfg(Mode.BOARD), now).single()
        assertNull(c.updatedAt); assertNull(c.arcPct)
        assertTrue(c.columns.all { it.second == WidgetModel.NONE })
    }

    @Test fun metricsKeepTheirOrder() {
        val order = listOf(Metric.IDLE, Metric.WEEK, Metric.WAITING)
        assertEquals(order, WidgetModel.cards(state, cfg(Mode.ACCOUNT, "personal", order), now).single().columns.map { it.first })
    }
}
