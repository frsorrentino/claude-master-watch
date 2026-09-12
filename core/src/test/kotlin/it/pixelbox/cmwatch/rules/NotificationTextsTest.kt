package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.QuotaAccount
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

class NotificationTextsTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val labels = NotificationTexts.Labels(open = "Apri", reply = "Rispondi", resetWeek = "reset settimana", window5h = "finestra 5 h")

    @Test fun question() {
        val n = NotificationTexts.question(s.sessions[0], labels)
        assertEquals("❓ ledger-api", n.title)
        assertEquals("Deploy ready, waiting for the client's ok. Deploy now?", n.body)
        assertEquals(listOf("1 · yes", "2 · no", "Apri"), n.actions)
        assertEquals(NotificationTexts.CHANNEL_QUESTIONS, n.channel)
    }

    @Test fun questionWithOneOptionHasOneActionPlusOpen() {
        val one = s.sessions[0].copy(question = s.sessions[0].question!!.copy(options = s.sessions[0].question!!.options.take(1)))
        assertEquals(listOf("1 · yes", "Apri"), NotificationTexts.question(one, labels).actions)
    }

    @Test fun outcome() {
        val n = NotificationTexts.outcome(s.sessions[1], labels)
        assertEquals("✓ atlas-shop", n.title); assertEquals("Migrazioni 008-011 applicate, test verdi", n.body)
        assertEquals(NotificationTexts.CHANNEL_OUTCOMES, n.channel)
    }

    @Test fun gone() {
        val n = NotificationTexts.gone("orbit-docs", labels)
        assertEquals("✗ orbit-docs", n.title); assertEquals("", n.body); assertEquals(NotificationTexts.CHANNEL_GONE, n.channel)
    }

    @Test fun quota() {
        val n = NotificationTexts.quota("personale", QuotaAccount(h5 = 95, w7 = 40, resetW7 = 1789610400), labels, ZoneId.of("Europe/Rome"))
        assertEquals("⚠ 95 % personale", n.title); assertEquals("finestra 5 h", n.body)
        val week = NotificationTexts.quota("agenzia", QuotaAccount(h5 = 10, w7 = 97, resetW7 = 1789610400), labels, ZoneId.of("Europe/Rome"))
        assertEquals("⚠ 97 % agenzia", week.title); assertEquals("reset settimana gio 04:00", week.body)
    }
}
