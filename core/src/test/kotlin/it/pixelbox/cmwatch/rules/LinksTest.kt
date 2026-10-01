package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** I link nei messaggi della chat, da rendere toccabili (Franz, 01/10 20:05). */
class LinksTest {
    private fun found(t: String) = Links.find(t).map { t.substring(it) }

    @Test fun findsPlainUrls() {
        assertEquals(listOf("https://claude.ai/artifact/RGdmD5fxUWUBLSbwWBBsCz"), found("Mockup: https://claude.ai/artifact/RGdmD5fxUWUBLSbwWBBsCz"))
    }

    @Test fun trailingPunctuationStaysOut() {
        assertEquals(listOf("https://a.it/x"), found("vedi https://a.it/x."))
        assertEquals(listOf("https://a.it/x"), found("(https://a.it/x)"))
        assertEquals(listOf("https://a.it/x"), found("«https://a.it/x»,"))
        assertEquals(listOf("https://a.it/x"), found("[link](https://a.it/x)"))
    }

    @Test fun keepsBalancedParenthesesAndQueries() {
        assertEquals(listOf("https://it.wikipedia.org/wiki/Roma_(città)"), found("https://it.wikipedia.org/wiki/Roma_(città) qui"))
        assertEquals(listOf("http://a.it/p?q=1&r=2#s"), found("http://a.it/p?q=1&r=2#s"))
    }

    @Test fun severalOrNone() {
        assertEquals(2, Links.find("https://a.it e https://b.it").size)
        assertEquals(0, Links.find("nessun link, solo claude.ai senza schema").size)
    }
}
