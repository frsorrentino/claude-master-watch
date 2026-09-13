package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.Night
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.util.Locale

class BriefCardsTest {
    private val state = ContractJson.decodeState(Fixtures.stateQuestion)
    private val labels = BriefCards.Labels(
        quota = "Quota %s", week = "settimana %s", resetAt = "reset %s", stale = "dato vecchio", none = "—",
        active = "Sessioni attive", waitingPill = "%d in attesa", noQuestions = "nessuna domanda",
        questions = "Domande aperte", oldest = "più vecchia %s",
        night = "Coda notte", running = "in corso %s", nothingRunning = "nessuna in corso",
        update = "Ultimo aggiornamento", minutes = "min", now = "ora", stopped = "PC fermo",
    )

    @Test fun personaleVienePrimaDegliAltriAccount() {
        val keys = BriefCards.quota(state, labels, ZoneId.of("Europe/Rome"), Locale.ITALIAN).map { it.key }
        assertEquals("quota-personale", keys.first())
        assertEquals(state.quota.size, keys.size)
    }

    @Test fun laCardDellaQuotaPortaPercentualeResetESettimana() {
        val c = BriefCards.quota(state, labels, ZoneId.of("Europe/Rome"), Locale.ITALIAN).first()
        val q = state.quota.getValue("personale")
        // Etichetta = solo l'account: «Quota» è l'intestazione della sezione e la colonna è strappa (13/09 16:41).
        assertEquals("personale", c.label)
        assertEquals(q.h5.toString(), c.value); assertEquals("%", c.unit)
        assertTrue(c.secondary!!.startsWith("reset "))
        assertEquals("settimana ${q.w7} %", c.pill)
        assertEquals(BriefCards.Tone.NEUTRAL, c.tone)
    }

    @Test fun quotaVecchiaDiceDatoVecchioEDiventaGrigia() {
        val stale = state.copy(quota = state.quota.mapValues { it.value.copy(stale = true) })
        val c = BriefCards.quota(stale, labels).first()
        assertEquals("dato vecchio", c.pill); assertEquals(BriefCards.Tone.STALE, c.tone)
    }

    @Test fun leAttiveContanoSoloChiLavoraELePillolineSeguonoLeDomande() {
        // fixture 1: ledger-api waiting con domanda, atlas-shop busy, field-notes idle, orbit-docs gone
        val cards = BriefCards.work(state, Freshness.Fresh, state.ts + 30, labels)
        val active = cards.first { it.key == "active" }
        assertEquals("2", active.value)
        assertEquals("1 in attesa", active.pill); assertEquals(BriefCards.Tone.WARN, active.tone)
    }

    @Test fun senzaDomandeLaPillolinaEBuona() {
        val calme = state.copy(sessions = state.sessions.map { it.copy(question = null) })
        val active = BriefCards.work(calme, Freshness.Fresh, calme.ts, labels).first { it.key == "active" }
        assertEquals("nessuna domanda", active.pill); assertEquals(BriefCards.Tone.GOOD, active.tone)
        assertTrue(BriefCards.work(calme, Freshness.Fresh, calme.ts, labels).none { it.key == "questions" })
    }

    @Test fun laCardDelleDomandeDiceLaPiuVecchia() {
        val c = BriefCards.work(state, Freshness.Fresh, state.ts + 600, labels).first { it.key == "questions" }
        val asked = state.sessions.mapNotNull { it.question }.minOf { it.askedAt }
        assertEquals("1", c.value)
        assertEquals("più vecchia " + Durations.since(asked, state.ts + 600), c.pill)
    }

    @Test fun aggiornamentoInMinutiEGrigioQuandoIlPcEFermo() {
        val fresco = BriefCards.work(state, Freshness.Fresh, state.ts, labels).first { it.key == "update" }
        assertEquals("ora", fresco.value); assertNull(fresco.unit); assertEquals(BriefCards.Tone.GOOD, fresco.tone)
        val fermo = BriefCards.work(state, Freshness.Stale(12), state.ts + 12 * 60, labels).first { it.key == "update" }
        assertEquals("12", fermo.value); assertEquals("min", fermo.unit)
        assertEquals("PC fermo", fermo.secondary); assertEquals(BriefCards.Tone.STALE, fermo.tone)
    }

    @Test fun laCodaDellaNotteCompareSoloQuandoCeQualcosa() {
        val senza = BriefCards.work(state.copy(night = Night()), Freshness.Fresh, state.ts, labels)
        assertTrue(senza.none { it.key == "night" })
        val con = BriefCards.work(state.copy(night = Night(queued = 3, running = "atlas-shop")), Freshness.Fresh, state.ts, labels)
        val c = con.first { it.key == "night" }
        assertEquals("3", c.value); assertEquals("in corso atlas-shop", c.pill)
    }
}

class BriefQuotaAlertTest {
    private val state = ContractJson.decodeState(Fixtures.stateQuestion)
    private val labels = BriefCards.Labels(
        quota = "Quota %s", week = "settimana %s", resetAt = "reset %s", stale = "dato vecchio", none = "—",
        active = "Sessioni attive", waitingPill = "%d in attesa", noQuestions = "nessuna domanda",
        questions = "Domande aperte", oldest = "più vecchia %s",
        night = "Coda notte", running = "in corso %s", nothingRunning = "nessuna in corso",
        update = "Aggiornato", minutes = "min", now = "ora", stopped = "PC fermo",
    )

    private fun tono(pct: Int): BriefCards.Tone {
        val s = state.copy(quota = mapOf("personale" to state.quota.getValue("personale").copy(h5 = pct, stale = false)))
        return BriefCards.quota(s, labels).first().tone
    }

    @Test fun scalaDiAllarmeSullaFinestraDiCinqueOre() {
        assertEquals(BriefCards.Tone.NEUTRAL, tono(50))
        assertEquals(BriefCards.Tone.NEUTRAL, tono(89))
        assertEquals(BriefCards.Tone.WARN, tono(90))
        assertEquals(BriefCards.Tone.ALERT, tono(100))
    }
}
