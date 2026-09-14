package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Tier
import it.pixelbox.cmwatch.rules.NotificationPlan.Act
import org.junit.Assert.*
import org.junit.Test

class NotificationPlanTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val l = NotificationPlan.Labels(open = "Apri", reply = "Rispondi", retry = "Riprova", read = "Leggi", stop = "Ferma", write = "Scrivi", resume = "Riprendi", sent = "inviato", confirmed = "confermato", notDelivered = "non consegnato", sessions = "sessioni")
    private val ledger = s.sessions[0]

    @Test fun questionMediumHasTwoDirectActionsReplyWithChoicesAndOpen() {
        val p = NotificationPlan.question(ledger, l, history = emptyList())
        assertEquals("❓ ledger-api", p.title)
        assertEquals("🔴 ledger-api", p.person)
        assertEquals("Deploy ready, waiting for the client's ok. Deploy now?", p.messages.last())
        assertEquals(listOf(Act.Option(1, "1 yes"), Act.Option(2, "2 no"), Act.Reply, Act.Open), p.actions)
        assertEquals(listOf("1 yes", "2 no"), p.choices)
        assertTrue(p.freeForm); assertTrue(p.chronometer); assertEquals(1789210500L, p.whenS)
        assertEquals(NotificationPlan.CH_QUESTIONS, p.channel); assertEquals("work", p.subText)
        assertFalse(p.autoCancel)
    }

    @Test fun questionHighHasOnlyOpen() {
        val high = ledger.copy(question = ledger.question!!.copy(tier = Tier.HIGH))
        val p = NotificationPlan.question(high, l, emptyList())
        assertEquals(listOf(Act.Open), p.actions); assertTrue(p.choices.isEmpty()); assertFalse(p.freeForm)
    }

    @Test fun historyStaysInTheThread() {
        val p = NotificationPlan.question(ledger, l, history = listOf(NotificationPlan.Qa("Run tests?", "1 yes", 1789200000L)))
        assertEquals(2, p.messages.size)
        assertEquals("❓ Run tests? → 1 yes", p.messages[0])
    }

    @Test fun afterTapTheSameNotificationSaysSentThenConfirmed() {
        assertEquals("✓ 1 · yes inviato", NotificationPlan.sentLine(1, "yes", l))
        assertEquals("✓ confermato", NotificationPlan.confirmedLine(l))
        assertEquals("✗ non consegnato · Riprova", NotificationPlan.failedLine(l))
    }

    // Mentre legge, il «Leggi» della notifica che ha fatto partire la voce dice «Ferma» (Franz, 14/09 16:08).
    @Test fun ilTastoLeggiDiventaFermaMentreLegge() {
        assertEquals("Ferma", NotificationPlan.readLabel(reading = true, l))
        assertEquals("Leggi", NotificationPlan.readLabel(reading = false, l))
    }

    @Test fun outcomeGoneQuota() {
        val o = NotificationPlan.outcome(s.sessions[1], l)
        assertEquals("✓ atlas-shop", o.title); assertEquals("Migrations 008-011 applied, tests green", o.messages.first())
        assertEquals("Esito: migrations 008-011 applied, tests green.\nThe test seeds and the admin page are still to review.", o.bigText)
        assertEquals(listOf(Act.Read, Act.Write, Act.Open), o.actions); assertTrue(o.autoCancel); assertEquals(12 * 3600_000L, o.timeoutMs)
        val g = NotificationPlan.gone("orbit-docs", "agenzia", l)
        assertEquals("✗ orbit-docs", g.title); assertEquals(listOf(Act.Resume), g.actions)
        val q = NotificationPlan.quota("personale", QuotaAccount(h5 = 95, w7 = 40, resetW7 = 1789610400), l)
        assertEquals("⚠ 95 % personale", q.title); assertEquals(95, q.progress); assertEquals(NotificationPlan.CH_QUOTA, q.channel)
    }

    @Test fun groupSummaryOneLinePerSession() {
        val lines = NotificationPlan.summary(s, l)
        assertEquals("4 sessioni · 1?", lines.title)
        assertEquals(listOf("❓ ledger-api", "▶ atlas-shop", "✓ field-notes", "✗ orbit-docs"), lines.rows)
    }
}
