package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.SessionState

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class SessionsTextTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val now = 1789210800L

    @Test fun waitingShowsAgeOfTheQuestion() = assertEquals("ledger-api · 5 m", SessionsText.row(s.sessions[0], now))
    @Test fun busyShowsTurnAge() = assertEquals("atlas-shop · 1 m", SessionsText.row(s.sessions[1], now))
    @Test fun idleShowsSinceAge() = assertEquals("field-notes · 1 g", SessionsText.row(s.sessions[2], now))
    @Test fun goneShowsNoAge() = assertEquals("orbit-docs", SessionsText.row(s.sessions[3], now))
    @Test fun headerCounts() = assertEquals("4 sessioni · 1 ❓ · 1 ✗", SessionsText.header(s.sessions, "sessioni"))
}

class SessionsSubTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)

    @Test fun laSessioneFinitaDiceCheEChiusaEDaQuando() {
        val gone = s.sessions.first { it.state == SessionState.GONE }
        assertEquals("chiusa · ${Durations.since(gone.since, gone.since + 7200)}", SessionsText.sub(gone, gone.since + 7200, "chiusa"))
    }

    @Test fun leAltreDannoSoloLEta() {
        val waiting = s.sessions.first { it.state == SessionState.WAITING }
        assertEquals(Durations.since(waiting.question!!.askedAt, waiting.since + 600), SessionsText.sub(waiting, waiting.since + 600, "chiusa"))
    }
}

class SessionsCellTest {
    private val st = ContractJson.decodeState(Fixtures.stateQuestion)
    private val tools = ToolText.Labels(
        run = "esegue %1\$s", read = "legge %1\$s", edit = "modifica %1\$s", write = "scrive %1\$s",
        search = "cerca %1\$s", web = "cerca sul web", message = "scrive a un'altra sessione",
        delegate = "delega a un agente", plan = "aggiorna il piano", other = "usa %1\$s",
    )

    @Test fun chiAspettaMostraLaDomandaComeTitolo() {
        val w = st.sessions.first { it.question != null }
        val c = SessionsText.cell(w, st.ts, "turno in corso", "a riposo", tools)
        assertEquals(w.question!!.text, c.title); assertNull(c.detail)
    }

    // `next_at` è la mezzanotte del giorno della riga di recap (contratto 1.2): la scheda mostra il prossimo solo se
    // la riga è di oggi, altrimenti niente (Franz, 14/09 10:42: «anche queste info sono stantie»).
    private val zone = ZoneId.of("Europe/Rome")
    private val oggi = Instant.ofEpochSecond(st.ts).atZone(zone).toLocalDate()
    private val mezzanotteOggi = oggi.atStartOfDay(zone).toEpochSecond()
    private val mezzanotteIeri = oggi.minusDays(1).atStartOfDay(zone).toEpochSecond()

    @Test fun chiLavoraMostraAttivitaEProssimoPasso() {
        val b = st.sessions.first { it.state == SessionState.BUSY }.copy(next = "rifinire la tile", nextAt = mezzanotteOggi)
        val c = SessionsText.cell(b, st.ts, "turno in corso", "a riposo", tools, zone)
        // Contratto 1.5: la description del comando, se c'è, vince sulla frase dallo strumento.
        assertEquals(ToolText.describe(b.toolNote, b.tool, tools), c.title)
        assertEquals("rifinire la tile", c.detail)
    }

    @Test fun ilProssimoDiIeriNonStaSottoLAttivita() {
        val b = st.sessions.first { it.state == SessionState.BUSY }.copy(next = "rifinire la tile", nextAt = mezzanotteIeri)
        assertNull(SessionsText.cell(b, st.ts, "turno in corso", "a riposo", tools, zone).detail)
    }

    @Test fun ilProssimoDiIeriNonFaDaTitoloAChiLavoraSenzaStrumento() {
        val b = st.sessions.first { it.state == SessionState.BUSY }
            .copy(tool = null, outcome = null, next = "rifinire la tile", nextAt = mezzanotteIeri)
        assertEquals("turno in corso", SessionsText.cell(b, st.ts, "turno in corso", "a riposo", tools, zone).title)
    }

    @Test fun ilProssimoDiIeriNonFaDaTitoloAChiEFerma() {
        val i = st.sessions.first { it.state == SessionState.IDLE }
            .copy(outcome = null, next = "rifinire la tile", nextAt = mezzanotteIeri)
        assertEquals("a riposo", SessionsText.cell(i, st.ts, "turno in corso", "a riposo", tools, zone).title)
    }

    @Test fun chiEFermaMostraLEsitoEIlProssimoPasso() {
        val i = st.sessions.first { it.state == SessionState.IDLE }
        val c = SessionsText.cell(i, st.ts, "turno in corso", "a riposo", tools)
        assertEquals(i.outcome?.short ?: "a riposo", c.title)
    }

    @Test fun unaChiusaMostraLUltimaCosaFatta() {
        val g = st.sessions.first { it.state == SessionState.GONE }
        val c = SessionsText.cell(g, st.ts, "turno in corso", "a riposo", tools)
        assertEquals(g.outcome?.short ?: g.project, c.title); assertNull(c.detail)
    }

    @Test fun unaChiusaSenzaEsitoMostraIlProgetto() {
        val g = st.sessions.first { it.state == SessionState.GONE }.copy(outcome = null)
        assertEquals(g.project, SessionsText.cell(g, st.ts, "turno in corso", "a riposo", tools).title)
    }
}

// Franz, 15/09 08:30: la lista tagliava a 42 caratteri, la misura della tile, anche quando sotto non c'era niente.
class SessionsTitleTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val now = 1789210800L
    private val rome = ZoneId.of("Europe/Rome")
    private val idle = s.sessions.first { it.state == SessionState.IDLE }
    private val ads = "- Passkey Google: il login Google la rifiuta come \"non compatibile\". Va risolta prima che serva un token Google nuovo, altrimenti il widget si blocca di nuovo."

    private fun cell(short: String, next: String? = null) = SessionsText.cell(
        idle.copy(outcome = idle.outcome!!.copy(short = short), next = next, nextAt = next?.let { now }),
        now, "turno in corso", "a riposo", zone = rome,
    )

    @Test fun senzaProssimoIlTitoloPrendeLeQuattroRighe() {
        val c = cell(ads)
        assertEquals(4, c.titleLines)
        assertEquals("Passkey Google: il login Google la rifiuta come \"non compatibile\".", c.titleText)
    }

    @Test fun unEsitoCortoSiLeggeIntero() =
        assertEquals("ricetta rilascio scritta in docs/metodologie", cell("ricetta rilascio scritta in docs/metodologie").titleText)

    @Test fun colProssimoPassoIlTitoloResta2Righe() {
        val c = cell("ricetta rilascio scritta in docs/metodologie", next = "rivedere i seed")
        assertEquals(2, c.titleLines)
        assertEquals("ricetta rilascio scritta in", c.titleText)
    }

    @Test fun ilSegnoDiElencoNonEntraNelTitolo() {
        assertEquals("Passkey Google", cell("- Passkey Google").title)
        assertEquals("Passkey Google", cell("* Passkey Google").title)
        assertEquals("Passkey Google", cell("1. Passkey Google").title)
    }

    @Test fun unLinkMarkdownDiventaIlSuoTesto() =
        assertEquals("Android Central – Googlebook event in New York", cell("- [Android Central – Googlebook event in New York](https://www.androidcentral.com/x)").title)
}
