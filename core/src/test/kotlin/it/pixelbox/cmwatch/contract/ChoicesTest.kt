package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChoicesTest {
    @Test fun choicesReadFromTheFixture() {
        val c = ContractJson.decodeState(Fixtures.stateQuestion).choices!!
        assertEquals("claude-fable-5-1", c.models[1].id)
        assertEquals(listOf("low", "medium", "high", "xhigh", "max"), c.efforts)
    }

    @Test fun olderRelayHasNoChoices() = assertNull(ContractJson.decodeState("""{"v":1,"ts":1,"host":"h"}""").choices)

    @Test fun opsReadFromTheFixture() =
        assertEquals(true, ContractJson.decodeState(Fixtures.stateQuestion).ops?.contains("interrupt"))

    @Test fun olderRelayHasNoOps() = assertNull(ContractJson.decodeState("""{"v":1,"ts":1,"host":"h"}""").ops)
}
