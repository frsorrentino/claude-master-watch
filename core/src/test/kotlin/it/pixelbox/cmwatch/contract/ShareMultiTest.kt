package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contratto 1.50 (Franz, 10/10 16:52: «due allegati insieme vengono mostrati ancora su 2 post»): un report, più allegati. */
class ShareMultiTest {
    @Test fun theStateSaysHowManyAttachmentsGoInOneReport() {
        for (j in listOf(Fixtures.stateQuestion, Fixtures.stateIdle, Fixtures.stateStale)) assertEquals(5, ContractJson.decodeState(j).share?.multi)
    }

    @Test fun moreAttachmentsGoTogetherOnlyWhenTheRelaySaysSo() {
        val st = ContractJson.decodeState(Fixtures.stateIdle).share
        assertTrue(it.pixelbox.cmwatch.rules.ShareLimits.together(st, 2)); assertTrue(it.pixelbox.cmwatch.rules.ShareLimits.together(st, 5))
        assertFalse(it.pixelbox.cmwatch.rules.ShareLimits.together(st, 1)); assertFalse(it.pixelbox.cmwatch.rules.ShareLimits.together(st?.copy(multi = null), 2))
    }

    @Test fun twoIdsTravelInOneReportAndComeBackAsOneResult() {
        val root = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject
        val cmds = root.getValue("cmd").jsonArray.map { ContractJson.json.decodeFromJsonElement(Cmd.serializer(), it) }
        val results = root.getValue("result").jsonArray.map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
        val two = cmds.single { it.id.endsWith("000000000500") }
        assertEquals(CmdOp.REPORT, two.op); assertEquals(2, two.arg.orEmpty().split(',').size)
        val ok = results.single { it.id == two.id }
        assertTrue(ok.ok); assertTrue(ok.text.startsWith("sent to atlas-shop: images saved as ")); assertEquals(2, ok.text.substringAfter("saved as ").split(", ").size)
        val missing = results.single { it.id.endsWith("000000000501") }
        assertFalse(missing.ok); assertEquals("image missing or unreadable", missing.text)
    }
}
