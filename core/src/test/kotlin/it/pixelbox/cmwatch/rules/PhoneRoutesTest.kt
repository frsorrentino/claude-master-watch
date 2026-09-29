package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.transport.DemoText
import it.pixelbox.cmwatch.transport.FakeTransport
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneRoutesTest {
    /** La prova che l'attestazione a Play («accesso completo») è vera: in Demo ogni rotta del telefono ha contenuto, senza rete. */
    @Test fun demoReachesEveryRouteOffline() = runTest {
        val fake = FakeTransport(load = { DemoText.dress(File("../contract/$it.json").readText()) })
        val st = fake.state.first()
        assertEquals(PhoneRoutes.Route.entries.toSet(), PhoneRoutes.reachable(st))
    }
}
