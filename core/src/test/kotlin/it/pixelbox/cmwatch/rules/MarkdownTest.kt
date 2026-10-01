package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

/** La formattazione dei messaggi della chat (Franz, 01/10 20:17: «**prova**» si vedeva con gli asterischi). */
class MarkdownTest {
    private fun styled(t: String, kind: Markdown.Kind): List<String> {
        val m = Markdown.parse(t)
        return m.spans.filter { it.kind == kind }.map { m.text.substring(it.range) }
    }

    @Test fun boldLosesItsAsterisks() {
        val m = Markdown.parse("una **prova** qui")
        assertEquals("una prova qui", m.text)
        assertEquals(listOf("prova"), styled("una **prova** qui", Markdown.Kind.BOLD))
    }

    @Test fun italicAndCode() {
        assertEquals("a b c", Markdown.parse("a *b* `c`").text)
        assertEquals(listOf("b"), styled("a *b* `c`", Markdown.Kind.ITALIC))
        assertEquals(listOf("c"), styled("a *b* `c`", Markdown.Kind.CODE))
    }

    @Test fun snakeCaseAndLoneAsterisksStay() {
        assertEquals("tool_note e 2 * 3 = 6", Markdown.parse("tool_note e 2 * 3 = 6").text)
        assertEquals("mcp__chrome-bridge__click", Markdown.parse("mcp__chrome-bridge__click").text)
    }

    @Test fun codeKeepsWhatIsInside() {
        val m = Markdown.parse("`**non grassetto**`")
        assertEquals("**non grassetto**", m.text)
        assertEquals(emptyList<String>(), m.spans.filter { it.kind == Markdown.Kind.BOLD }.map { m.text.substring(it.range) })
    }

    @Test fun listsAndHeadings() {
        val m = Markdown.parse("## Titolo\n- **Uno**: primo\n* due")
        assertEquals("Titolo\n• Uno: primo\n• due", m.text)
        assertEquals(listOf("Titolo", "Uno"), styled("## Titolo\n- **Uno**: primo\n* due", Markdown.Kind.BOLD))
    }

    @Test fun linkWithText() {
        val m = Markdown.parse("vedi [il mockup](https://claude.ai/artifact/X).")
        assertEquals("vedi il mockup.", m.text)
        val link = m.spans.single { it.kind == Markdown.Kind.LINK }
        assertEquals("il mockup", m.text.substring(link.range))
        assertEquals("https://claude.ai/artifact/X", link.url)
    }

    @Test fun codeBlockFencesGo() {
        val m = Markdown.parse("prima\n```bash\nls -la\n```\ndopo")
        assertEquals("prima\nls -la\ndopo", m.text)
        assertEquals(listOf("ls -la"), styled("prima\n```bash\nls -la\n```\ndopo", Markdown.Kind.CODE))
    }

    @Test fun unclosedMarkersStayAsTheyAre() {
        assertEquals("un **solo", Markdown.parse("un **solo").text)
    }
}
