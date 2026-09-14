package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.TileTexts.Card
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class TileCardsTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)

    @Test fun onlyNonZeroCountersAtMostThree() {
        // fixture 1: ledger-api waiting (con domanda), atlas-shop busy, field-notes idle, orbit-docs gone
        assertEquals(listOf(Card(Card.Kind.ACTIVE, 2), Card(Card.Kind.IDLE, 1), Card(Card.Kind.GONE, 1)), TileTexts.cards(q, emptySet()))
        assertEquals(listOf(Card(Card.Kind.IDLE, 1)), TileTexts.cards(idle, emptySet()))
    }

    @Test fun waitingCountsAsActiveWhenTheQuestionWasSeen() {
        val seen = setOf(q.sessions[0].question!!.id)
        assertEquals(listOf(Card(Card.Kind.ACTIVE, 2), Card(Card.Kind.IDLE, 1), Card(Card.Kind.GONE, 1)), TileTexts.cards(q, seen))
    }

    @Test fun neverMoreThanThree() {
        val many = q.copy(sessions = q.sessions + q.sessions.map { it.copy(id = it.id + "x", name = it.name + "x", state = SessionState.AWAITING, question = null) })
        assertTrue(TileTexts.cards(many, emptySet()).size <= 3)
    }

    @Test fun badgeUsesTheContractEmojiElseTheStateGlyph() {
        assertEquals("🟦", TileTexts.badge(q.sessions[0]))
        assertEquals("❓", TileTexts.badge(q.sessions[0].copy(icon = null)))
        assertEquals("▶", TileTexts.badge(q.sessions[1].copy(icon = null, question = null)))
        assertEquals("✗", TileTexts.badge(q.sessions[3].copy(icon = null)))
    }

    @Test fun questionAgeLine() {
        assertEquals("ferma da 5 m", TileTexts.waitingFor(q.sessions[0], 1789210800, "ferma da %s"))
    }
}

class TileRestTest {
    private val q = it.pixelbox.cmwatch.contract.ContractJson.decodeState(it.pixelbox.cmwatch.Fixtures.stateQuestion)

    @Test fun chiLavoraVinceSempre() {
        val r = TileTexts.rest(q, q.ts) as TileTexts.Rest.Live
        assertTrue(r.busy)
        assertEquals("ledger-api", r.session.name)      // seguita e in attesa di risposta
    }

    @Test fun senzaNessunoAlLavoroValeLUltimoMovimentoSeRecente() {
        val calme = q.copy(sessions = q.sessions.map { it.copy(state = SessionState.IDLE, question = null, followed = false) })
        val r = TileTexts.rest(calme, calme.sessions.maxOf { it.since } + 60) as TileTexts.Rest.Live
        assertFalse(r.busy)
    }

    @Test fun esitoVecchioNonEUnaNotizia() {
        val calme = q.copy(sessions = q.sessions.map { it.copy(state = SessionState.IDLE, question = null, followed = false) })
        val tardi = calme.sessions.maxOf { maxOf(it.since, it.turnStarted ?: 0L, it.outcome?.at ?: 0L) } + TileTexts.REST_FRESH_S + 1
        val r = TileTexts.rest(calme, tardi) as TileTexts.Rest.Calm
        assertEquals(calme.sessions.count { it.state != SessionState.GONE }, r.sessions)
        assertNotNull(r.since)
    }
}

class TileActivityTest {
    private val q = it.pixelbox.cmwatch.contract.ContractJson.decodeState(it.pixelbox.cmwatch.Fixtures.stateQuestion)
    private val busy = q.sessions.first { it.state == SessionState.BUSY }

    @Test fun mentreLavoraValeLAttivitaDiAdesso() {
        // Contratto 1.5: la description del comando, se c'è, vince sul comando grezzo.
        assertEquals(busy.toolNote ?: busy.tool, TileTexts.activity(busy, busy = true, running = "turno in corso", idle = "a riposo"))
    }

    @Test fun senzaAttivitaValeLEsitoSePiuFrescoDelTurno() {
        val s = busy.copy(tool = null, turnStarted = 1000L, next = "vecchio prossimo passo")
            .let { it.copy(outcome = it.outcome?.copy(short = "migrazioni applicate", at = 2000L)) }
        assertEquals("migrazioni applicate", TileTexts.activity(s, busy = true, running = "turno in corso", idle = "a riposo"))
    }

    @Test fun conEsitoVecchioRestaIlProssimoPasso() {
        val s = busy.copy(tool = null, turnStarted = 5000L, next = "rivedere i seed")
            .let { it.copy(outcome = it.outcome?.copy(at = 1000L)) }
        assertEquals("rivedere i seed", TileTexts.activity(s, busy = true, running = "turno in corso", idle = "a riposo"))
    }

    @Test fun ilTestoSiTagliaAllaPrimaFrase() {
        assertEquals("dal polso: «Pubblica».", TileTexts.primaFrase("dal polso: «Pubblica». Release 0.4.0 avviata (in corso)"))
        val lunga = "una frase molto lunga che supera la riga e mezza prima del punto. coda"
        assertEquals(lunga, TileTexts.primaFrase(lunga))
    }
}

class TileNextAtTest {
    private val q = it.pixelbox.cmwatch.contract.ContractJson.decodeState(it.pixelbox.cmwatch.Fixtures.stateQuestion)
    private val busy = q.sessions.first { it.state == SessionState.BUSY }

    @Test fun laFixtureDella12PortaLaDataDelProssimo() {
        assertEquals(1789171200L, busy.nextAt)
        assertNull(q.sessions.first { it.name == "field-notes" }.nextAt)
    }

    @Test fun unProssimoVecchioNonSiMostra() {
        val s = busy.copy(tool = null, next = "rivedere i seed", nextAt = 1789171200L, turnStarted = 1789171300L)
            .let { it.copy(outcome = null) }
        val tardi = 1789171200L + TileTexts.NEXT_MAX_AGE_S + 1
        assertEquals("turno in corso", TileTexts.activity(s, busy = true, running = "turno in corso", idle = "a riposo", now = tardi))
    }

    @Test fun unProssimoDiOggiSiMostraSeLEsitoEPiuVecchio() {
        val s = busy.copy(tool = null, next = "rivedere i seed", nextAt = 1789171200L, turnStarted = 1789171300L)
            .let { it.copy(outcome = it.outcome?.copy(at = 1789000000L)) }
        assertEquals("rivedere i seed", TileTexts.activity(s, busy = true, running = "turno in corso", idle = "a riposo", now = 1789200000L))
    }

    @Test fun senzaDataIlProssimoNonSiUsa() {
        val s = busy.copy(tool = null, next = "rivedere i seed", nextAt = null, outcome = null)
        assertEquals("turno in corso", TileTexts.activity(s, busy = true, running = "turno in corso", idle = "a riposo", now = 1789200000L))
    }
}

class TileQuotasTest {
    private val q = it.pixelbox.cmwatch.contract.ContractJson.decodeState(it.pixelbox.cmwatch.Fixtures.stateQuestion)

    // Una riga sola, dell'account scelto nelle impostazioni (Franz, 14/09 11:33: con due righe la seconda era tagliata).
    @Test fun laRigaDellaQuotaEDellAccountScelto() {
        val idle = ContractJson.decodeState(Fixtures.stateIdle)
        val r = TileTexts.quotaLine(idle, "agenzia")!!
        assertEquals("agenzia", r.account); assertFalse(r.personale)
        assertEquals(idle.quota.getValue("agenzia").h5, r.pct)
    }

    @Test fun unAccountCheNonCeRipiegaSuPersonale() {
        val r = TileTexts.quotaLine(q, "lavoro")!!
        assertEquals("personale", r.account); assertTrue(r.personale)
    }

    @Test fun senzaQuoteNienteRiga() = assertNull(TileTexts.quotaLine(q.copy(quota = emptyMap()), "personale"))

    @Test fun inCodaPercentualeERipartenzaDelleCinqueOre() {
        val r = TileTexts.quotaLine(q, "personale")!!
        assertEquals("11 % · 18:00", TileTexts.quotaSuffix(r, "%1\$d %%", "%1\$d %% · %2\$s", ZoneId.of("Europe/Rome")))
    }

    @Test fun senzaRipartenzaSoloLaPercentuale() {
        val r = TileTexts.quotaLine(q, "personale")!!.copy(resetH5 = null)
        assertEquals("11 %", TileTexts.quotaSuffix(r, "%1\$d %%", "%1\$d %% · %2\$s", ZoneId.of("Europe/Rome")))
    }
}

class TileAwaitingTest {
    private val q = it.pixelbox.cmwatch.contract.ContractJson.decodeState(it.pixelbox.cmwatch.Fixtures.stateQuestion)

    @Test fun unaSessioneInAttesaDiRispostaLoDice() {
        val a = q.sessions.first { it.state == SessionState.BUSY }
            .copy(state = SessionState.AWAITING, tool = null, next = null, outcome = null)
        assertEquals(
            "prompt dal polso in corso",
            TileTexts.activity(a, busy = true, running = "turno in corso", idle = "a riposo", awaiting = "prompt dal polso in corso"),
        )
    }
}
