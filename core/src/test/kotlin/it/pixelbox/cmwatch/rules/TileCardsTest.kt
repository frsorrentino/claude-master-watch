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

    // Nella tile due righe e mai «…» (Franz, 14/09 16:58): un pensiero intero, non una coda troncata.
    @Test fun nellaTileUnTestoCortoResta() = assertEquals("build installata", TileTexts.fitTile("build installata"))

    @Test fun nellaTileLaFraseCheFinisceEntroIlLimite() =
        assertEquals("Build installata alle 16:42.", TileTexts.fitTile("Build installata alle 16:42. Poi lint vital in CI su 472868f."))

    @Test fun nellaTileSenzaPuntoSiFermaAllaVirgola() = assertEquals(
        "nessun errore",
        TileTexts.fitTile("nessun errore, il bordo arriva con la build in corso; lo stato del relay segna la sessione"),
    )

    @Test fun nellaTileMaiPuntiniEMaiOltreIlLimite() {
        val t = TileTexts.fitTile((1..30).joinToString(" ") { "parola$it" })
        assertFalse(t.contains("…")); assertTrue(t.length <= TileTexts.TILE_MAX); assertTrue(t.startsWith("parola1 parola2"))
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
        val r = TileTexts.quotaLine(idle, "work")!!
        assertEquals("work", r.account); assertFalse(r.personale)
        assertEquals(idle.quota.getValue("work").h5, r.pct)
    }

    @Test fun unAccountCheNonCeRipiegaSuPersonale() {
        val r = TileTexts.quotaLine(q, "lavoro")!!
        // Contratto 1.8: il ripiego va sull'account di tipo personal, qualunque sia il suo nome.
        assertEquals("personal", r.account); assertTrue(r.personale)
    }

    // La quota segue la sessione mostrata sopra (Franz, 14/09 15:26): sessione di lavoro, quota di lavoro.
    @Test fun laQuotaEDellAccountDellaSessioneMostrata() {
        val s = q.sessions.first { it.state == SessionState.BUSY }
        assertEquals(s.account, TileTexts.quotaAccount(TileTexts.Rest.Live(s, busy = true), "un-altro"))
    }

    @Test fun senzaSessioneMostrataLaQuotaDelleImpostazioni() =
        assertEquals("agenzia", TileTexts.quotaAccount(TileTexts.Rest.Calm(sessions = 2, since = null), "agenzia"))

    @Test fun senzaQuoteNienteRiga() = assertNull(TileTexts.quotaLine(q.copy(quota = emptyMap()), "personale"))

    @Test fun laRigaPortaAncheLaSettimana() {
        val r = TileTexts.quotaLine(q, "personale")!!
        val p = q.quota.getValue("personal")
        assertEquals(p.w7, r.w7); assertEquals(p.resetW7, r.resetW7)
    }

    // Franz, 15/09 08:21: sotto il 15 % la barra non dice niente e ruba spazio alla sessione. Ma la tile guardava solo
    // le cinque ore: con la settimana al 66 % e le cinque ore al 2 % l'avrebbe nascosta proprio quando conta.
    @Test fun sottoSogliaLaQuotaNonSiMostra() {
        val r = TileTexts.quotaLine(q, "personale")!!.copy(pct = 14, w7 = 79)
        assertNull(TileTexts.tileQuota(r))
    }

    @Test fun leCinqueOreDal15PerCento() {
        val r = TileTexts.quotaLine(q, "personale")!!.copy(pct = 15, w7 = 36)
        assertEquals(TileTexts.TileQuota(TileTexts.Window.H5, 15, r.resetH5), TileTexts.tileQuota(r))
    }

    @Test fun laSettimanaDall80PerCento() {
        val r = TileTexts.quotaLine(q, "personale")!!.copy(pct = 2, w7 = 80)
        assertEquals(TileTexts.TileQuota(TileTexts.Window.WEEK, 80, r.resetW7), TileTexts.tileQuota(r))
    }

    @Test fun seSonoTutteESopraSogliaVinceLaPiuPiena() {
        val r = TileTexts.quotaLine(q, "personale")!!
        assertEquals(TileTexts.Window.WEEK, TileTexts.tileQuota(r.copy(pct = 40, w7 = 85))!!.window)
        assertEquals(TileTexts.Window.H5, TileTexts.tileQuota(r.copy(pct = 90, w7 = 85))!!.window)
    }

    private val formati = TileTexts.QuotaLabels(pct = "%1\$d %%", pctReset = "%1\$d %% · %2\$s", week = "settimana %1\$d %%", weekReset = "settimana %1\$d %% · %2\$s")
    private val rome = ZoneId.of("Europe/Rome")

    @Test fun inCodaPercentualeERipartenzaDelleCinqueOre() {
        val r = TileTexts.quotaLine(q, "personale")!!
        assertEquals("11 % · 18:00", TileTexts.quotaSuffix(TileTexts.TileQuota(TileTexts.Window.H5, 11, r.resetH5), formati, rome))
    }

    @Test fun senzaRipartenzaSoloLaPercentuale() =
        assertEquals("11 %", TileTexts.quotaSuffix(TileTexts.TileQuota(TileTexts.Window.H5, 11, null), formati, rome))

    // S07: «0 % · 17:30» non diceva che 17:30 è il reset delle cinque ore. Letto dalle stringhe vere dell'app.
    // Franz, 15/09 08:27: con la settimana sulla tile anche le cinque ore dicono di essere le cinque ore.
    @Test fun laTileDiceCheLOraEIlResetDelleCinqueOre() {
        val r = TileTexts.quotaLine(q, "personale")!!
        val t = TileTexts.TileQuota(TileTexts.Window.H5, 11, r.resetH5)
        assertEquals("5 ore 11 % · reset 18:00", TileTexts.quotaSuffix(t, labels("values"), rome))
        assertEquals("5 h 11% · reset 18:00", TileTexts.quotaSuffix(t, labels("values-en"), rome, java.util.Locale.ENGLISH))
    }

    // La settimana sulla tile non c'era mai stata: deve dire che è la settimana e il giorno del reset.
    @Test fun laSettimanaLoDiceEHaIlGiorno() {
        val r = TileTexts.quotaLine(q, "personale")!!
        val t = TileTexts.TileQuota(TileTexts.Window.WEEK, 82, r.resetW7)
        assertEquals("settimana 82 % · reset gio 04:00", TileTexts.quotaSuffix(t, labels("values"), rome, java.util.Locale.ITALIAN))
        assertEquals("week 82% · reset Thu 04:00", TileTexts.quotaSuffix(t, labels("values-en"), rome, java.util.Locale.ENGLISH))
    }

    // Con due barre «settimana 68 % · reset gio 04:00» non stava accanto alla barra, che spariva (Franz, 15/09 12:29):
    // etichette corte e solo l'ora del reset.
    @Test fun conDueBarreLeRigheSonoCorte() {
        val r = TileTexts.quotaLine(q, "personale")!!
        val h5 = TileTexts.TileQuota(TileTexts.Window.H5, 0, r.resetH5)
        val week = TileTexts.TileQuota(TileTexts.Window.WEEK, 68, r.resetW7)
        assertEquals("5 h 0 % · 18:00", TileTexts.quotaSuffix(h5, shortLabels("values"), rome, java.util.Locale.ITALIAN))
        assertEquals("7 g 68 % · gio 04:00", TileTexts.quotaSuffix(week, shortLabels("values"), rome, java.util.Locale.ITALIAN))
        assertEquals("5 h 0% · 18:00", TileTexts.quotaSuffix(h5, shortLabels("values-en"), rome, java.util.Locale.ENGLISH))
        assertEquals("7 d 68% · Thu 04:00", TileTexts.quotaSuffix(week, shortLabels("values-en"), rome, java.util.Locale.ENGLISH))
    }

    private fun shortLabels(dir: String) = TileTexts.QuotaLabels(
        pct = res(dir, "tile_quota_h5_short"), pctReset = res(dir, "tile_quota_h5_short_reset"),
        week = res(dir, "tile_quota_week_short"), weekReset = res(dir, "tile_quota_week_short_reset"),
    )

    private fun labels(dir: String) = TileTexts.QuotaLabels(
        pct = res(dir, "tile_quota_pct"), pctReset = res(dir, "tile_quota_pct_reset"),
        week = res(dir, "tile_quota_week"), weekReset = res(dir, "tile_quota_week_reset"),
    )

    private fun res(dir: String, name: String): String {
        val xml = java.io.File("../wear/src/main/res/$dir/strings.xml").readText()
        return Regex("<string name=\"$name\">(.*?)</string>").find(xml)!!.groupValues[1]
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

// Franz, 15/09 10:56: la quota nascosta sotto soglia lasciava righe vuote. Decide lo spazio: con un testo che sta in
// due righe la quota c'è sempre; sparisce solo per un testo lungo con la quota sotto soglia, che prende quattro righe.
class TileBodyTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val busy = q.sessions.first { it.state == SessionState.BUSY }
    private val sotto = TileTexts.quotaLine(q, "personale")!!.copy(pct = 2, w7 = 36)
    private val sopra = sotto.copy(pct = 90)
    private val lungo = "Dal server il file arriva intero e integro: l'ho riscaricato e controllato con la sua impronta."

    // Franz, 15/09 11:58: quando il testo lo consente, sotto anche la barra della settimana.
    @Test fun unTestoCortoHaLeDueBarre() {
        val b = TileTexts.tileBody("modifica watch-install-release-signing.md", null, sotto)
        assertEquals(listOf(TileTexts.TileQuota(TileTexts.Window.H5, 2, sotto.resetH5), TileTexts.TileQuota(TileTexts.Window.WEEK, 36, sotto.resetW7)), b.quotas)
        assertEquals(2, b.mainLines); assertNull(b.extra)
    }

    @Test fun laSettimanaSopraSogliaConTestoCortoHaLeDueBarre() {
        val b = TileTexts.tileBody("turno in corso", null, sotto.copy(pct = 40, w7 = 85))
        assertEquals(listOf(TileTexts.Window.H5, TileTexts.Window.WEEK), b.quotas.map { it.window })
    }

    // 15/09 12:15: il relay manda `h5: null` (nessuna lettura delle cinque ore) e la settimana al 67 %: la tile restava
    // senza nessuna barra. Senza le cinque ore c'è la settimana.
    @Test fun senzaCinqueOreNelDatoLaBarraDellaSettimana() {
        val corto = TileTexts.tileBody("turno in corso", null, sotto.copy(pct = null, w7 = 67))
        assertEquals(listOf(TileTexts.TileQuota(TileTexts.Window.WEEK, 67, sotto.resetW7)), corto.quotas)
        val esteso = TileTexts.tileBody(lungo, null, sotto.copy(pct = null, w7 = 67))
        assertEquals(listOf(TileTexts.Window.WEEK), esteso.quotas.map { it.window })
    }

    @Test fun senzaSettimanaNelDatoUnaBarraSola() {
        val b = TileTexts.tileBody("turno in corso", null, sotto.copy(w7 = null))
        assertEquals(listOf(TileTexts.Window.H5), b.quotas.map { it.window }); assertEquals(3, b.mainLines)
    }

    @Test fun testoEAggiuntaCortiStannoConLaQuota() {
        val b = TileTexts.tileBody("modifica Notifier.kt", "rilasciata la 0.4.7", sotto)
        assertEquals("rilasciata la 0.4.7", b.extra); assertEquals(1, b.extraLines)
        assertEquals(listOf(TileTexts.Window.H5, TileTexts.Window.WEEK), b.quotas.map { it.window })
    }

    // Franz, 15/09 11:37: la quarta riga la tile la taglia; ci stanno tre righe e la barra. La quota c'è sempre.
    @Test fun unTestoLungoStaInTreRigheSopraLaQuota() {
        val b = TileTexts.tileBody("modifica Notifier.kt", lungo, sotto)
        assertEquals(listOf(TileTexts.TileQuota(TileTexts.Window.H5, 2, sotto.resetH5)), b.quotas)
        assertEquals(1, b.mainLines); assertEquals(2, b.extraLines)
        assertEquals(TileTexts.fitTile(lungo, 2 * TileTexts.TILE_LINE), b.extra)
    }

    @Test fun unEsitoLungoDaSoloPrendeTreRighe() {
        val b = TileTexts.tileBody(lungo, null, sotto)
        assertEquals(3, b.mainLines); assertEquals(TileTexts.fitTile(lungo, 3 * TileTexts.TILE_LINE), b.main)
        assertEquals(1, b.quotas.size)
    }

    @Test fun sopraSogliaLaQuotaRestaConLeStesseTreRighe() {
        val b = TileTexts.tileBody("modifica Notifier.kt", lungo, sopra)
        assertEquals(listOf(TileTexts.Window.H5), b.quotas.map { it.window }); assertEquals(90, b.quotas[0].pct)
        assertEquals(1, b.mainLines); assertEquals(2, b.extraLines)
    }

    @Test fun senzaQuotaNelDatoLeRigheRestanoTre() {
        val b = TileTexts.tileBody("modifica Notifier.kt", lungo, null)
        assertTrue(b.quotas.isEmpty()); assertEquals(2, b.extraLines)
    }

    // «- [Android Central – Googlebook event in New» sulla tile (Franz, 15/09 11:37): markdown grezzo dall'esito.
    @Test fun ilMarkdownDellEsitoDiventaTestoSemplice() {
        assertEquals("Android Central – Googlebook event in New York", TileTexts.plain("- [Android Central – Googlebook event in New York](https://www.androidcentral.com/x)"))
        assertEquals("tile rifatta, quotaLine nuova", TileTexts.plain("**tile** rifatta, `quotaLine` nuova"))
        assertEquals("1 · yes", TileTexts.plain("1 · yes"))
    }

    @Test fun lEsitoSullaTileEPulito() {
        val idle = q.sessions.first { it.state == SessionState.IDLE }
        val s = idle.copy(outcome = idle.outcome!!.copy(short = "- [Googlebook](https://x.y/z) presentato"), next = null)
        assertEquals("Googlebook presentato", TileTexts.activity(s, busy = false, running = "turno in corso", idle = "a riposo"))
    }

    @Test fun conUnoStrumentoInCorsoSottoVaLUltimoEsito() {
        val s = busy.copy(tool = "Edit core/x.kt", toolNote = null, next = null, nextAt = null,
            outcome = busy.outcome!!.copy(short = "migrazioni applicate, test verdi", at = 100L))
        assertEquals("migrazioni applicate, test verdi", TileTexts.extra(s, busy = true, now = 200L))
    }

    @Test fun senzaStrumentoNienteAggiunta() {
        val s = busy.copy(tool = null, outcome = busy.outcome!!.copy(short = "migrazioni applicate", at = 100L))
        assertNull(TileTexts.extra(s, busy = true, now = 200L))
        assertNull(TileTexts.extra(s, busy = false, now = 200L))
    }

    // «watch-install-release-s» tagliato di lato: un nome senza spazi non andava a capo.
    @Test fun iNomiLunghiVannoACapoDopoITrattini() {
        assertEquals("watch-​install-​release-​signing.md", TileTexts.breakable("watch-install-release-signing.md"))
        assertEquals("claude-​master-​watch", TileTexts.breakable("claude-master-watch"))
        assertEquals("a - b", TileTexts.breakable("a - b"))
    }
}
