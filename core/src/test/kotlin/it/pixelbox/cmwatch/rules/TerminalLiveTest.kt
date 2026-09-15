package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test

class TerminalLiveTest {
    private fun s(state: SessionState = SessionState.BUSY, tool: String? = "Bash pytest -q") =
        Session("1", "atlas-shop", "personal", "p", state, since = 100L, tool = tool)

    @Test fun allAperturaNiente() = assertNull(TerminalLive.next(null, s()))
    @Test fun fermaNiente() = assertNull(TerminalLive.next(s(), s()))
    @Test fun nuovoStrumentoCattura() = assertEquals(TerminalLive.Ask.SCREEN, TerminalLive.next(s(), s(tool = "Read a.kt")))
    @Test fun nuovaNotaCattura() = assertEquals(TerminalLive.Ask.SCREEN, TerminalLive.next(s(), s().copy(toolNote = "Run the tests")))
    @Test fun fineTurnoAncheLaRisposta() = assertEquals(TerminalLive.Ask.SCREEN_AND_LAST, TerminalLive.next(s(), s(SessionState.IDLE, null)))
    @Test fun domandaAncheLaRisposta() = assertEquals(TerminalLive.Ask.SCREEN_AND_LAST, TerminalLive.next(s(), s(SessionState.WAITING)))
    @Test fun sessioneSparitaNiente() = assertNull(TerminalLive.next(s(), null))
}
