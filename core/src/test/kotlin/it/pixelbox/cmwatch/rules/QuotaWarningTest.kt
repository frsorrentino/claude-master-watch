package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.State
import org.junit.Assert.*
import org.junit.Test

/**
 * L'avviso della quota sopra la barra di scrittura (piano 30/09, Task 3): solo per l'account della sessione, solo con una
 * lettura delle 5 ore fresca e un reset futuro. Fixture: atlas-shop è personale (h5 11, reset fra 5 ore), ledger-api è
 * di lavoro (h5 assente, stale).
 */
class QuotaWarningTest {
    private val base = ContractJson.decodeState(Fixtures.stateQuestion)
    private val now = base.ts
    private val reset = base.quota.getValue("personal").resetH5!!
    private val atlas = base.sessions.first { it.name == "atlas-shop" }
    private fun personal(q: QuotaAccount): State = base.copy(quota = base.quota + ("personal" to q))

    @Test fun warnsAtNinety() {
        val st = personal(base.quota.getValue("personal").copy(h5 = 94))
        val w = QuotaWarning.of(st, atlas, emptyList(), now)!!
        assertEquals("personal", w.account); assertEquals(94, w.pct); assertEquals(reset, w.resetAt); assertFalse(w.projected)
    }

    @Test fun noWarningBelowNinetyAtAnEvenPace() {
        val st = personal(base.quota.getValue("personal").copy(h5 = 40))
        assertNull(QuotaWarning.of(st, atlas, emptyList(), now))
    }

    /** La finestra è appena ripartita: un'ora dopo è al 40 %, 30 punti in un'ora; al reset (fra 4 ore) si va oltre il 100 %. */
    @Test fun warnsWhenThePaceRunsOut() {
        val st = personal(base.quota.getValue("personal").copy(h5 = 40))
        val samples = listOf(QuotaHistory.Sample(now, 10), QuotaHistory.Sample(now + 3600, 40))
        val w = QuotaWarning.of(st, atlas, samples, now + 3600)!!
        assertTrue(w.projected); assertEquals(40, w.pct)
    }

    @Test fun noWarningWithoutAFreshReading() {
        val p = base.quota.getValue("personal")
        assertNull(QuotaWarning.of(personal(p.copy(h5 = 95, stale = true)), atlas, emptyList(), now))
        assertNull(QuotaWarning.of(personal(p.copy(h5 = null)), atlas, emptyList(), now))
        assertNull(QuotaWarning.of(personal(p.copy(h5 = 95, resetH5 = null)), atlas, emptyList(), now))
        assertNull(QuotaWarning.of(personal(p.copy(h5 = 95, resetH5 = now - 60)), atlas, emptyList(), now))
    }

    @Test fun otherAccountDoesNotWarn() {
        // Personale al 99 %, lavoro fresco al 50 %: la sessione di lavoro non vede l'avviso dell'altro account.
        val st = personal(base.quota.getValue("personal").copy(h5 = 99)).let { it.copy(quota = it.quota + ("work" to it.quota.getValue("work").copy(h5 = 50, stale = false))) }
        val ledger = base.sessions.first { it.name == "ledger-api" }
        assertNull(QuotaWarning.of(st, ledger, emptyList(), now))
    }

    // Segnalazione 01/10 21:58: «mostra 1% e mi avvisa già che a questo ritmo non arriva alle 02:50?». Con la finestra
    // appena ripartita due campioni vicini danno un ritmo enorme: il giudizio aspetta mezz'ora di campioni e il 20 %.
    @Test fun noPaceWarningOnTooLittleEvidence() {
        val p = base.quota.getValue("personal")
        val early = listOf(QuotaHistory.Sample(now, 0), QuotaHistory.Sample(now + 120, 1))
        assertNull(QuotaWarning.of(personal(p.copy(h5 = 1)), atlas, early, now + 120))
        val short = listOf(QuotaHistory.Sample(now, 10), QuotaHistory.Sample(now + 600, 30))
        assertNull(QuotaWarning.of(personal(p.copy(h5 = 30)), atlas, short, now + 600))
        val low = listOf(QuotaHistory.Sample(now, 2), QuotaHistory.Sample(now + 3600, 15))
        assertNull(QuotaWarning.of(personal(p.copy(h5 = 15)), atlas, low, now + 3600))
    }
}
