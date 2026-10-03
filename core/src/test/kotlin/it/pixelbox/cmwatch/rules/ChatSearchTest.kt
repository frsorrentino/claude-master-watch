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

    // Contratto 1.27: con la ricerca del relay i messaggi e gli esiti sono già nelle conversazioni; del telefono restano
    // solo le voci del registro senza sessione. Tutto dal più recente.
    @Test fun conversationsTakeThePlaceOfTheLocalMessages() {
        val local = listOf(
            ChatSearch.Hit("kb", 300, "deploy di kb", 0, 6, ChatSearch.Kind.SENT, "SENT-1"),
            ChatSearch.Hit("kb", 200, "deploy finito", 0, 6, ChatSearch.Kind.EVENT, "EVENT-e1"),
            ChatSearch.Hit(null, 400, "Oggi: deploy di atlas", 6, 12, ChatSearch.Kind.EVENT, "EVENT-e2"),
        )
        val page = it.pixelbox.cmwatch.contract.SearchPage(listOf(
            it.pixelbox.cmwatch.contract.SearchHit("atlas", true, entry = "a1.0", at = 500, snippet = "il deploy è partito", match = listOf(3, 9)),
            it.pixelbox.cmwatch.contract.SearchHit("old", false, entry = "o1.0", at = 100, snippet = "deploy", match = listOf(0, 6)),
        ))
        val hits = ChatSearch.withConversations(local, page)
        assertEquals(listOf("atlas", null, "old"), hits.map { it.session })
        assertEquals(listOf(ChatSearch.Kind.CONVERSATION, ChatSearch.Kind.EVENT, ChatSearch.Kind.CONVERSATION), hits.map { it.kind })
        assertEquals(listOf(true, null, false), hits.map { it.live })
        assertEquals("deploy", hits[0].line.substring(hits[0].start, hits[0].end))
    }

    @Test fun aMatchOutsideTheSnippetIsClamped() {
        val page = it.pixelbox.cmwatch.contract.SearchPage(listOf(it.pixelbox.cmwatch.contract.SearchHit("a", true, at = 1, snippet = "abc", match = listOf(2, 9))))
        val h = ChatSearch.withConversations(emptyList(), page).single()
        assertEquals(2, h.start); assertEquals(3, h.end)
    }
}
