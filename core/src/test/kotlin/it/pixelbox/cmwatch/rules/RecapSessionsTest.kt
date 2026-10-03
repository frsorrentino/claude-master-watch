package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.RecapSessions.Kind
import org.junit.Assert.*
import org.junit.Test

/** Il recap del Registro diviso per sessione (segnalazione 03/10 17:52: un blocco per sessione, icona giusta). */
class RecapSessionsTest {
    // Il formato di `render_short` del plugin: titolo, sezioni in maiuscolo, una riga per sessione con icona, nome, link.
    private val it = """
        Recap 02/10/2026 · 4 progetti · 1 ferme su domanda

        FERME SU UNA DOMANDA

        🟧 ledger-api (https://claude.ai/code/session_01A) · AskUserQuestion
             Deploy ready, waiting for the client's ok.

        APERTE

        ⚪ master (https://claude.ai/code/session_01B): La sessione rino è avviata, nuova.
             ↳ prossimo: prova la casa dal vivo

        🟢 claude-master (https://claude.ai/code/session_01C): Contratto 1.26 fatto.

        CHIUSE OGGI

        ✓ sito.⁠com: Pagina dei prezzi rifatta.

        altro: kb, bozze

        Progetti: master 12, claude-master 9
    """.trimIndent()

    @Test fun sessionsInTheirSections() {
        val v = RecapSessions.parse(it)
        assertEquals("Recap 02/10/2026 · 4 progetti · 1 ferme su domanda", v.title)
        assertEquals(listOf(Kind.WAITING, Kind.OPEN, Kind.CLOSED, Kind.OTHER), v.sections.map { s -> s.kind })
        val waiting = v.sections[0].entries.single()
        assertEquals("ledger-api", waiting.name); assertEquals("🟧", waiting.icon); assertEquals("AskUserQuestion", waiting.tool)
        assertEquals("https://claude.ai/code/session_01A", waiting.url); assertEquals("Deploy ready, waiting for the client's ok.", waiting.detail)
        val master = v.sections[1].entries[0]
        assertEquals("master", master.name); assertEquals("La sessione rino è avviata, nuova.", master.text); assertEquals("prova la casa dal vivo", master.next)
        assertEquals(listOf("master", "claude-master"), v.sections[1].entries.map { e -> e.name })
        val closed = v.sections[2]
        assertEquals("sito.com", closed.entries.single().name); assertEquals("Pagina dei prezzi rifatta.", closed.entries.single().text)
        assertEquals(listOf("altro: kb, bozze"), closed.lines)
        assertEquals(listOf("Progetti: master 12, claude-master 9"), v.sections[3].lines)
    }

    // Il formato vecchio della fixture del contratto, in inglese.
    @Test fun theOldEnglishFormatToo() {
        val v = RecapSessions.parse("Recap 12/09/2026 · 3 projects · 1 waiting on a question\n\nWAITING ON A QUESTION\n⏳ ledger-api · since 14:15 · AskUserQuestion\n   Deploy ready, waiting for the client's ok.\n\nOPEN\n● atlas-shop · 6 turns · last 13:45\n   Migrations 008-011 applied, tests green")
        assertEquals(listOf(Kind.WAITING, Kind.OPEN), v.sections.map { s -> s.kind })
        assertEquals("since 14:15 · AskUserQuestion", v.sections[0].entries.single().tool)
        assertEquals("Migrations 008-011 applied, tests green", v.sections[1].entries.single().detail)
    }

    // Tondo = personale, quadrato = lavoro, come i badge di Telegram.
    @Test fun theShapeComesFromTheEmoji() {
        assertTrue(RecapSessions.personal("🟢")); assertTrue(RecapSessions.personal("⚪"))
        assertFalse(RecapSessions.personal("🟧")); assertFalse(RecapSessions.personal("🟩"))
    }
}
