package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.*
import org.junit.Test

class BadgeTest {
    @Test fun shapeFollowsTheAccount() {
        assertEquals(Badge.Shape.CIRCLE, Badge.of("personale", null, SessionState.BUSY).shape)
        assertEquals(Badge.Shape.SQUARE, Badge.of("agenzia", null, SessionState.BUSY).shape)
    }

    @Test fun fillIsTheSessionColourOrGrey() {
        assertEquals(0xFF4C7DFF.toInt(), Badge.of("personale", "#4C7DFF", SessionState.IDLE).fill)
        assertEquals(0xFF9B9B9B.toInt(), Badge.of("personale", null, SessionState.IDLE).fill)
        assertEquals(0xFF9B9B9B.toInt(), Badge.of("personale", "rosso", SessionState.IDLE).fill)   // valore non valido → grigio
    }

    @Test fun iconEmojiGivesShapeAndColourWhenColorIsMissing() {
        val sq = Badge.of("personale", null, SessionState.IDLE, icon = "🟦")
        assertEquals(Badge.Shape.CIRCLE, sq.shape); assertEquals(0xFF3B82F6.toInt(), sq.fill)     // forma dall'account, non dall'emoji
        val ci = Badge.of("agenzia", null, SessionState.IDLE, icon = "🟢")
        assertEquals(Badge.Shape.SQUARE, ci.shape); assertEquals(0xFF2ECC71.toInt(), ci.fill)
        assertEquals(0xFFE74C3C.toInt(), Badge.of("personale", "#E74C3C", SessionState.IDLE, icon = "🟢").fill)   // color vince sull'emoji
        assertEquals(Badge.Shape.SQUARE, Badge.of("professionale", null, SessionState.IDLE).shape)
    }

    @Test fun contractFixturesCarryIconAndColour() {
        val s = it.pixelbox.cmwatch.contract.ContractJson.decodeState(it.pixelbox.cmwatch.Fixtures.stateQuestion)
        assertEquals("🟦", s.sessions[0].icon); assertEquals("#3B82F6", s.sessions[0].color)
        assertEquals(0xFF3B82F6.toInt(), Badge.of(s.sessions[0].account, s.sessions[0].color, s.sessions[0].state, s.sessions[0].icon).fill)
        assertEquals(Badge.Shape.SQUARE, Badge.of(s.sessions[0].account, s.sessions[0].color, s.sessions[0].state).shape)   // agenzia
    }

    @Test fun glyphPerState() {
        assertEquals(Badge.Glyph.PLAY, Badge.of("personale", null, SessionState.BUSY).glyph)
        assertEquals(Badge.Glyph.PLAY, Badge.of("personale", null, SessionState.AWAITING).glyph)
        assertEquals(Badge.Glyph.CHECK, Badge.of("personale", null, SessionState.IDLE).glyph)
        assertEquals(Badge.Glyph.QUESTION, Badge.of("personale", null, SessionState.WAITING).glyph)
        assertEquals(Badge.Glyph.CROSS, Badge.of("personale", null, SessionState.GONE).glyph)
    }

    // Respira ogni sessione che lavora, come il pallino dell'app Claude (Franz, 14/09 16:24), non solo la seguita.
    @Test fun respiranoLeSessioniCheLavorano() {
        assertTrue(Badge.breathes(SessionState.BUSY)); assertTrue(Badge.breathes(SessionState.AWAITING))
        assertFalse(Badge.breathes(SessionState.IDLE)); assertFalse(Badge.breathes(SessionState.WAITING)); assertFalse(Badge.breathes(SessionState.GONE))
    }

    @Test fun glyphColourByContrastNotByTable() {
        assertEquals(Badge.BLACK, Badge.of("personale", "#FFB020", SessionState.WAITING).glyphColor)   // ambra chiara → nero
        assertEquals(Badge.WHITE, Badge.of("personale", "#1A237E", SessionState.WAITING).glyphColor)   // blu scuro → bianco
        assertEquals(Badge.BLACK, Badge.of("personale", "#9B9B9B", SessionState.IDLE).glyphColor)      // grigio: nero contrasta ≥ 4,5
        assertTrue(Badge.contrast(0xFFFFFFFF.toInt(), 0xFF000000.toInt()) > 20.0)
        assertEquals(1.0, Badge.contrast(0xFF777777.toInt(), 0xFF777777.toInt()), 0.001)
    }
}
