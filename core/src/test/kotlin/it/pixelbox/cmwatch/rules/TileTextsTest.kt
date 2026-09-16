package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class TileTextsTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)
    private val idle = ContractJson.decodeState(Fixtures.stateIdle)
    private val now = 1789210800L

    @Test fun header() {
        assertEquals("4 sessioni · 1? · 1✗", TileTexts.header(q, "sessioni"))
        assertEquals("1 sessioni", TileTexts.header(idle, "sessioni"))
    }

    @Test fun lineIsTheStuckSessionWithTheWholeQuestion() =
        assertEquals("❓ ledger-api · Deploy ready, waiting for the client's ok. Deploy now?", TileTexts.line(q, now))

    @Test fun lineWithoutQuestionsIsTheFollowedOrMostRecent() {
        assertEquals("✓ atlas-shop · 6 m", TileTexts.line(idle, 1789214000L + 6 * 60))
        val followed = q.copy(sessions = q.sessions.map { it.copy(question = null, state = if (it.name == "ledger-api") SessionState.BUSY else it.state) })
        assertEquals("▶ ledger-api · 7 m", TileTexts.line(followed, now))   // seguita, turn_started 7 m fa
    }

    @Test fun buttons() {
        assertEquals(listOf(TileTexts.Button.OPEN, TileTexts.Button.SESSIONS), TileTexts.buttons(q))
        assertEquals(listOf(TileTexts.Button.SESSIONS, TileTexts.Button.QUOTA), TileTexts.buttons(idle))
    }

    @Test fun freshness() {
        assertEquals(30_000L, TileTexts.freshnessMs(q))
        assertEquals(15 * 60_000L, TileTexts.freshnessMs(idle))
    }

    /**
     * N2 (Franz, 16/09 08:13): la tile diceva «fermo da 9 min» e la lista «da 17». La tile scrive l'età come testo e si
     * ridisegnava ogni 15 minuti; con il PC fermo si ridisegna ogni minuto, così l'età scritta non resta indietro.
     */
    @Test fun conIlPcFermoLaTileSiRidisegnaOgniMinuto() {
        assertEquals(60_000L, TileTexts.freshnessMs(idle, Freshness.Stale(9)))
        assertEquals(60_000L, TileTexts.freshnessMs(q, Freshness.Stale(9)))
        assertEquals(15 * 60_000L, TileTexts.freshnessMs(idle, Freshness.Fresh))
    }

    @Test fun staleLine() = assertEquals("PC fermo da 12 min", TileTexts.staleLine(Freshness.Stale(12), "PC fermo da %d min"))
}
