package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardTextTest {
    private val s = ContractJson.decodeState(Fixtures.stateQuestion)
    private val labels = mapOf(SessionState.WAITING to "In attesa di risposta", SessionState.BUSY to "Lavora", SessionState.IDLE to "Ferma", SessionState.GONE to "Sparita")

    @Test fun header() = assertEquals("ledger-api · work · In attesa di risposta · 5 m", CardText.header(s.sessions[0], 1789210800, labels))
    @Test fun busyShowsTool() = assertEquals("atlas-shop · personal · Lavora · 1 m · Bash pytest -q tests", CardText.header(s.sessions[1], 1789210800, labels))
    @Test fun goneHasNoDuration() = assertEquals("orbit-docs · work · Sparita", CardText.header(s.sessions[3], 1789210800, labels))
    @Test fun next() = assertEquals("→ Wait for the go", CardText.next(s.sessions[0]))
    @Test fun noNext() = assertNull(CardText.next(s.sessions[2]))
}
