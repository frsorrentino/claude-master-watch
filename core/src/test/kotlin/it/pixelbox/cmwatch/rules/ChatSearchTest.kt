package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import org.junit.Assert.*
import org.junit.Test

/** La ricerca (piano 30/09, Task 5): nei messaggi mandati, negli esiti dei loro turni e negli eventi del diario. */
class ChatSearchTest {
    private val sent = listOf(
        Sent("a", "kb", "Perché i test falliscono?", sentAt = 100, outcomeFull = "Mancava la fixture.\nOra i test passano.", doneAt = 160),
        Sent("b", "atlas", "deploy staging", sentAt = 300),
    )
    private val events = listOf(
        Event("e1", EventKind.OUTCOME, session = "kb", ts = 200, title = "kb", body = "Migrazione del database finita"),
        Event("e2", EventKind.RECAP, ts = 400, title = "Diario", body = "Oggi: deploy di atlas e test di kb"),
    )

    @Test fun findsInSentAndOutcomes() {
        val hits = ChatSearch.find("fixture", sent, emptyList())
        assertEquals(1, hits.size)
        assertEquals("kb", hits[0].session); assertEquals("Mancava la fixture.", hits[0].line); assertEquals(ChatSearch.Kind.OUTCOME, hits[0].kind)
        assertEquals("fixture", hits[0].line.substring(hits[0].start, hits[0].end))
        assertEquals(ChatSearch.Kind.SENT, ChatSearch.find("staging", sent, emptyList()).single().kind)
    }

    @Test fun findsInDiaryEvents() {
        val hits = ChatSearch.find("database", sent, events)
        assertEquals(ChatSearch.Kind.EVENT, hits.single().kind)
        assertEquals(200L, hits.single().at)
    }

    /** «perche» trova «Perché», e la parte in grassetto è quella del testo originale. */
    @Test fun accentsAndCaseIgnored() {
        val h = ChatSearch.find("PERCHE", sent, events).single()
        assertEquals("Perché", h.line.substring(h.start, h.end))
    }

    @Test fun newestFirst() {
        assertEquals(listOf(400L, 300L), ChatSearch.find("deploy", sent, events).map { it.at })
    }

    @Test fun blankFindsNothing() = assertTrue(ChatSearch.find("  ", sent, events).isEmpty())

    /** Revisione finale 01/10 (I4): due risultati uguali (stessa frase nello stesso secondo) hanno chiavi diverse. */
    @Test fun sameLineTwiceHasDistinctRefs() {
        val twice = listOf(Sent("x1", "kb", "continua", sentAt = 500), Sent("x2", "kb", "continua", sentAt = 500))
        val hits = ChatSearch.find("continua", twice, emptyList())
        assertEquals(2, hits.map { it.ref }.toSet().size)
    }
}
