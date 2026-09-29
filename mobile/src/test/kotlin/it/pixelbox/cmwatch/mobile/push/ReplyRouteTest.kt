package it.pixelbox.cmwatch.mobile.push

import org.junit.Assert.*
import org.junit.Test

class ReplyRouteTest {
    @Test fun optionAction() = assertEquals(ReplyRoute.Cmd.Option("kb", 2), ReplyRoute.from(ReplyRoute.OPTION, "kb", 2, null))
    @Test fun textAction() = assertEquals(ReplyRoute.Cmd.Text("kb", "sì"), ReplyRoute.from(ReplyRoute.REPLY, "kb", 0, " sì "))
    @Test fun blankTextIsNothing() = assertNull(ReplyRoute.from(ReplyRoute.REPLY, "kb", 0, "  "))
    @Test fun missingSessionIsNothing() = assertNull(ReplyRoute.from(ReplyRoute.OPTION, null, 1, null))
}
