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
        assertEquals("quota-personal", keys.first())
        assertEquals(state.quota.size, keys.size)
    }

    @Test fun laCardDellaQuotaPortaPercentualeResetESettimana() {
        val c = BriefCards.quota(state, labels, ZoneId.of("Europe/Rome"), Locale.ITALIAN).first()
        val q = state.quota.getValue("personal")
        // Etichetta = solo l'account: «Quota» è l'intestazione della sezione e la colonna è strappa (13/09 16:41).
        assertEquals("personal", c.label)
        assertEquals(q.h5.toString(), c.value); assertEquals("%", c.unit)
        // Sotto la percentuale delle 5 ore va la ripartenza delle 5 ore (`reset_h5`, le 18:00 a Roma), non quella
        // settimanale: lì «gio 04:00» sembrava il reset delle 5 ore (Franz, 14/09 10:38: «dovrebbe essere 12:30»).
        assertEquals("reset 18:00", c.secondary)
        // La ripartenza settimanale sta sotto la pillolina, su una riga sua: dentro andava a capo (14/09 11:33).
        assertEquals("settimana ${q.w7} %", c.pill)
        assertEquals("reset gio 04:00", c.note)
        assertEquals(BriefCards.Tone.NEUTRAL, c.tone)
    }

    // Anello concentrico (proposta 46, fase 1): fuori le 5 ore, dentro la settimana, così le due misure si raccontano
    // uguali invece di essere una un numero grande e l'altra una pillolina.
    @Test fun laCardDellaQuotaPortaAncheLaFrazioneDellaSettimana() {
        val c = BriefCards.quota(state, labels, ZoneId.of("Europe/Rome"), Locale.ITALIAN).first()
        val q = state.quota.getValue("personal")
        assertEquals(q.h5!! / 100f, c.progress!!, 0.001f)
        assertEquals(q.w7!! / 100f, c.progress2!!, 0.001f)
    }

    @Test fun senzaCinqueOreLAnelloEsternoELaSettimanaEDentroNonCEniente() {
        val q = state.quota.getValue("personal")
        val s = state.copy(quota = mapOf("personal" to q.copy(h5 = null, resetH5 = null)))
        val c = BriefCards.quota(s, labels.copy(weekOnly = "settimana"), ZoneId.of("Europe/Rome"), Locale.ITALIAN).first()
        assertEquals(q.w7!! / 100f, c.progress!!, 0.001f)
        assertNull(c.progress2)
    }

    @Test fun senzaRipartenzaDelleCinqueOreLaRigaDelResetNonSiDisegna() {
        val s = state.copy(quota = mapOf("personal" to state.quota.getValue("personal").copy(resetH5 = null)))
        assertNull(BriefCards.quota(s, labels, ZoneId.of("Europe/Rome"), Locale.ITALIAN).first().secondary)
    }

    // Franz, 15/09 15:34: senza lettura delle 5 ore (`h5: null`) la card mostrava solo «—». Il numero grande diventa la
    // settimana, con il suo reset sotto; la pillolina dice che è la settimana, senza ripetere il numero.
    @Test fun senzaCinqueOreIlNumeroGrandeELaSettimana() {
        val q = state.quota.getValue("personal")
        val s = state.copy(quota = mapOf("personal" to q.copy(h5 = null, resetH5 = null)))
        val c = BriefCards.quota(s, labels.copy(weekOnly = "settimana"), ZoneId.of("Europe/Rome"), Locale.ITALIAN).first()
        assertEquals(q.w7.toString(), c.value); assertEquals("%", c.unit)
        assertEquals("reset gio 04:00", c.secondary)
        assertEquals("settimana", c.pill)
        assertNull(c.note)
        assertEquals(BriefCards.Tone.NEUTRAL, c.tone)
    }

    @Test fun senzaCinqueOreLaSettimanaAllOttantaPerCentoEDaGuardare() {
        val q = state.quota.getValue("personal")
        val s = state.copy(quota = mapOf("personal" to q.copy(h5 = null, resetH5 = null, w7 = 85)))
        val c = BriefCards.quota(s, labels.copy(weekOnly = "settimana"), ZoneId.of("Europe/Rome"), Locale.ITALIAN).first()
        assertEquals(BriefCards.Tone.WARN, c.tone)
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
        val s = state.copy(quota = mapOf("personal" to state.quota.getValue("personal").copy(h5 = pct, stale = false)))
        return BriefCards.quota(s, labels).first().tone
    }

    @Test fun scalaDiAllarmeSullaFinestraDiCinqueOre() {
        assertEquals(BriefCards.Tone.NEUTRAL, tono(50))
        assertEquals(BriefCards.Tone.NEUTRAL, tono(89))
        assertEquals(BriefCards.Tone.WARN, tono(90))
        assertEquals(BriefCards.Tone.ALERT, tono(100))
    }
}

/**
 * Il tono dell'anello interno, la settimana (Franz, 16/09 11:52: settimana all'82 % e l'anello interno dello stesso
 * azzurro delle 5 ore, senza avviso). Ambra dall'80 %, la soglia di stop; rosso esaurita; spento col dato vecchio.
 */
class BriefQuotaWeekToneTest {
    private val state = ContractJson.decodeState(Fixtures.stateQuestion)
    private val labels = BriefCards.Labels(
        quota = "Quota %s", week = "settimana %s", resetAt = "reset %s", stale = "dato vecchio", none = "—",
        active = "Sessioni attive", waitingPill = "%d in attesa", noQuestions = "nessuna domanda",
        questions = "Domande aperte", oldest = "più vecchia %s",
        night = "Coda notte", running = "in corso %s", nothingRunning = "nessuna in corso",
        update = "Aggiornato", minutes = "min", now = "ora", stopped = "PC fermo",
    )

    private fun card(w7: Int, stale: Boolean = false) = BriefCards.quota(
        state.copy(quota = mapOf("personal" to state.quota.getValue("personal").copy(h5 = 9, w7 = w7, stale = stale))), labels,
    ).first()

    @Test fun laSettimanaHaIlSuoTono() {
        assertEquals(BriefCards.Tone.NEUTRAL, card(79).tone2)
        assertEquals(BriefCards.Tone.WARN, card(82).tone2)
        assertEquals(BriefCards.Tone.ALERT, card(100).tone2)
        assertEquals(BriefCards.Tone.STALE, card(82, stale = true).tone2)
    }

    /** Le 5 ore al 9 % restano tranquille anche con la settimana all'82 %: i due anelli non si copiano il tono. */
    @Test fun iDueToniSonoIndipendenti() {
        assertEquals(BriefCards.Tone.NEUTRAL, card(82).tone)
    }
}
