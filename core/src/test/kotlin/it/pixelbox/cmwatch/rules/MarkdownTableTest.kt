package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.rules.MarkdownTable.Card
import it.pixelbox.cmwatch.rules.MarkdownTable.Table
import org.junit.Assert.*
import org.junit.Test

/** Le tabelle nel testo (Franz, 02/10 21:07: con le barre e i trattini non si leggevano; approvato alle 21:11). */
class MarkdownTableTest {
    // La tabella dello screenshot del 02/10 21:04.
    private val numeri = """
        I numeri

        | cosa | ora | a mezzogiorno |
        |---|---|---|
        | post del film (01/10) | 223 visualizzazioni, 4 repost, 0 Mi piace | 121 visualizzazioni, 1 repost |
        | 6 risposte di oggi | 78 visualizzazioni in tutto (fra 3 e 28 l'una), 0 Mi piace, 0 risposte | — |
        | nuovi follower | 1 | — |

        Su GitHub nessuna visita arriva da X.
    """.trimIndent()

    @Test fun aTableIsItsOwnBlockBetweenTheText() {
        val b = MarkdownTable.blocks(numeri)
        assertEquals(3, b.size)
        assertEquals(MarkdownTable.Text("I numeri"), b[0])
        val t = b[1] as Table
        assertEquals(listOf("cosa", "ora", "a mezzogiorno"), t.header)
        assertEquals(3, t.rows.size); assertEquals("1", t.rows[2][1])
        assertEquals(MarkdownTable.Text("Su GitHub nessuna visita arriva da X."), b[2])
    }

    @Test fun pipesWithoutTheDashesLineAreText() {
        val src = "| a | b |\n| 1 | 2 |"
        assertEquals(listOf(MarkdownTable.Text(src)), MarkdownTable.blocks(src))
    }

    @Test fun aNarrowTableStaysAGridAWideOneBecomesCards() {
        assertTrue(MarkdownTable.compact(Table(listOf("file", "righe"), listOf(listOf("Repo.kt", "336"), listOf("Slash.kt", "47")))))
        assertFalse(MarkdownTable.compact(MarkdownTable.blocks(numeri)[1] as Table))
    }

    // Una scheda per riga: la prima colonna fa da titolo, le altre «intestazione: valore»; le celle vuote o «—» si saltano.
    @Test fun cardsOneRowEachWithoutEmptyCells() {
        val cards = MarkdownTable.cards(MarkdownTable.blocks(numeri)[1] as Table)
        assertEquals(Card("post del film (01/10)", listOf("ora: 223 visualizzazioni, 4 repost, 0 Mi piace", "a mezzogiorno: 121 visualizzazioni, 1 repost")), cards[0])
        assertEquals(Card("nuovi follower", listOf("ora: 1")), cards[2])
    }

    @Test fun anEscapedPipeStaysInItsCell() {
        val t = MarkdownTable.blocks("| a | b |\n|---|---|\n| x \\| y | z |")[0] as Table
        assertEquals(listOf("x | y", "z"), t.rows[0])
    }

    // A voce le righe come le schede, senza barre né trattini.
    @Test fun spokenReadsTheCards() {
        val spoken = MarkdownTable.spoken(numeri)
        assertTrue(spoken.contains("post del film (01/10). ora: 223 visualizzazioni, 4 repost, 0 Mi piace. a mezzogiorno: 121 visualizzazioni, 1 repost."))
        assertFalse(spoken.contains("|")); assertFalse(spoken.contains("---"))
        assertTrue(spoken.startsWith("I numeri")); assertTrue(spoken.endsWith("arriva da X."))
    }
}
