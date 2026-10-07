package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.LiveCard.Buzz
import it.pixelbox.cmwatch.rules.LiveCard.Kind
import it.pixelbox.cmwatch.rules.LiveTap.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveCardTest {
    @Test fun everyCardKindGoesThereAndBack() {
        for (k in Kind.entries) {
            val c = LiveCard(
                seq = 7, kind = k, title = "ledger api", text = "Deploy now?", options = listOf("yes", "no"),
                blocking = listOf(true, false), buzz = Buzz.LONG, until = 1_789_220_005_000, onlyBlocking = true,
            )
            assertEquals(c, LiveWire.card(LiveWire.encode(c)))
        }
    }

    @Test fun everyTapGoesThereAndBack() {
        for (a in Action.entries) {
            val t = LiveTap(a, index = 2, text = "di' a atlas: rivedi i semi")
            assertEquals(t, LiveWire.tap(LiveWire.encode(t)))
        }
    }

    // Un watch o un telefono più nuovo può mandare campi in più: si ignorano.
    @Test fun unknownFieldsAreIgnored() {
        val raw = """{"seq":1,"kind":"IDLE","later":"x"}""".toByteArray()
        assertEquals(LiveCard(seq = 1, kind = Kind.IDLE), LiveWire.card(raw))
        assertEquals(LiveTap(Action.REPEAT), LiveWire.tap("""{"action":"REPEAT","extra":1}""".toByteArray()))
    }
}
