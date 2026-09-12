package it.pixelbox.cmwatch.transport

import it.pixelbox.cmwatch.Fixtures
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SwitchableTransportTest {
    @Test fun switchingChangesTheStreamedState() = runTest {
        val a = FakeTransport({ Fixtures.read("$it.json") }); val b = FakeTransport({ Fixtures.read("$it.json") }).apply { useFixture("state-2-idle") }
        val s = SwitchableTransport(a)
        assertEquals(4, s.state.first().sessions.size)
        s.switchTo(b)
        assertEquals(1, s.state.first().sessions.size)
        assertEquals(1, s.fetchState().sessions.size)
    }
}
