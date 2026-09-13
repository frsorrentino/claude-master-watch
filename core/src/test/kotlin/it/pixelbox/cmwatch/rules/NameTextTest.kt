package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

class NameTextTest {
    private val visible = listOf("claude-master", "claude-master-watch", "master", "chrome-bridge")

    @Test fun namesSharingAPrefixMustNotBeCutAtTheEnd() {
        assertTrue(NameText.sharesPrefix("claude-master", visible))
        assertTrue(NameText.sharesPrefix("claude-master-watch", visible))
        assertFalse(NameText.sharesPrefix("master", visible))
        assertFalse(NameText.sharesPrefix("chrome-bridge", visible))
    }

    @Test fun aloneNeverCollides() = assertFalse(NameText.sharesPrefix("claude-master", listOf("claude-master")))

    @Test fun middleShortenKeepsTheDistinguishingTail() {
        assertEquals("claude-master", NameText.shorten("claude-master", visible, 13))
        assertEquals("claude…-watch", NameText.shorten("claude-master-watch", visible, 13))
        assertEquals("claude·-watch", NameText.shorten("claude-master-watch", visible, 13, ellipsis = "·"))
        assertEquals("chrome-bri…", NameText.shorten("chrome-bridge", listOf("chrome-bridge"), 11))   // senza collisione: coda
        assertEquals("master", NameText.shorten("master", visible, 20))                               // entra intero
    }

    @Test fun shortenNeverReturnsMoreThanMax() {
        for (n in visible) for (m in 4..24) assertTrue(NameText.shorten(n, visible, m).length <= m)
    }
}
