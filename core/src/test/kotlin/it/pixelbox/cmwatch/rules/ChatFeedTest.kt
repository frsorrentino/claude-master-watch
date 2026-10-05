package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test

class ChatFeedTest {
    private val results = Json.parseToJsonElement(Fixtures.cmdResult).jsonObject.getValue("result").jsonArray
        .map { ContractJson.json.decodeFromJsonElement(CmdResult.serializer(), it) }
    private val first = ContractJson.decodeTranscript(results[19].text)
    private val after = ContractJson.decodeTranscript(results[20].text)

    @Test fun parsesTheFixturePage() {
        assertEquals(18, first.entries.size)
        assertFalse(first.more)
        assertEquals("image/png", first.entries.first { it.id == "a6.0" }.files!!.single().mime)
        assertEquals(130L, first.entries.first { it.id == "a4.0" }.turn!!.out)
    }

    @Test fun phoneMessageBecomesMineWithItsStatus() {
        val p1 = first.entries.first { it.id == "p1.0" }
        val sent = Sent("c1", "field-notes", "Also add the attendees list", sentAt = p1.at!! - 2)
        val feed = ChatFeed.merge(first.entries, listOf(sent to ChatRules.Status.DONE))
        val mine = feed.filterIsInstance<ChatFeed.Item.Mine>().single()
        assertEquals("p1.0", mine.entry!!.id)
        assertEquals(ChatRules.Status.DONE, mine.status)
        assertTrue(feed.none { it is ChatFeed.Item.User && it.entry.id == "p1.0" })
    }

    @Test fun textTypedAtThePcIsNeverMine() {
        val sent = Sent("c3", "field-notes", "Also fix the typo in the title", sentAt = 1789207318)
        val feed = ChatFeed.merge(first.entries, listOf(sent to ChatRules.Status.DELIVERED))
        assertTrue(feed.any { it is ChatFeed.Item.User && it.entry.id == "u2.0" })
        assertNull(feed.filterIsInstance<ChatFeed.Item.Mine>().single().entry)
    }

    @Test fun prefixedUserEntryStillMatches() {
        val e = TranscriptEntry("u9.0", "user", text = "Dall'utente via polso (watch). Chiudi con Watch. run the tests", at = 100)
        val feed = ChatFeed.merge(listOf(e), listOf(Sent("c9", "kb", "run the tests", sentAt = 99) to ChatRules.Status.DELIVERED))
        assertTrue(feed.single() is ChatFeed.Item.Mine)
    }

    @Test fun unmatchedSentGoesByTime() {
        val sent = Sent("c2", "field-notes", "one more thing", sentAt = 1789207400)
        val feed = ChatFeed.merge(first.entries, listOf(sent to ChatRules.Status.SENDING))
        val last = feed.last() as ChatFeed.Item.Mine
        assertNull(last.entry)
        assertEquals("c2", last.sent.id)
    }

    @Test fun rolesMapToItems() {
        val feed = ChatFeed.merge(first.entries, emptyList())
        assertTrue(feed.first() is ChatFeed.Item.User)
        assertTrue(feed.first { (it as? ChatFeed.Item.Tool)?.entry?.id == "a3.0" } is ChatFeed.Item.Tool)
        assertTrue(feed.last() is ChatFeed.Item.Claude)
    }

    @Test fun afterPageAppendsWithoutDuplicates() {
        val merged = ChatFeed.append(first.entries.take(5), after, ChatFeed.Page.AFTER)
        assertEquals(first.entries.map { it.id }, merged.map { it.id })
    }

    @Test fun beforePagePrepends() {
        val older = TranscriptPage(listOf(TranscriptEntry("u0.0", "user", text = "earlier", at = 1)), more = false)
        assertEquals("u0.0", ChatFeed.append(first.entries, older, ChatFeed.Page.BEFORE).first().id)
    }

    @Test fun argForThePolls() {
        assertEquals("50", ChatFeed.arg(null))
        assertEquals("50:after=a5.0", ChatFeed.arg("a5.0"))
        assertEquals("50:before=u1.0", ChatFeed.olderArg("u1.0"))
    }

    // Revisione 30/09: una risposta corta ripetuta va al messaggio più vicino nel tempo, non al più vecchio.
    @Test fun nearestMessageWins() {
        val e = TranscriptEntry("u5.0", "user", text = "sì", at = 1000, origin = "phone")
        val old = Sent("a", "kb", "sì", sentAt = 100) to ChatRules.Status.DELIVERED
        val new = Sent("b", "kb", "sì", sentAt = 998) to ChatRules.Status.DELIVERED
        val mine = ChatFeed.merge(listOf(e), listOf(old, new)).filterIsInstance<ChatFeed.Item.Mine>()
        assertEquals("u5.0", mine.single { it.sent.id == "b" }.entry?.id)
    }

    @Test fun watchEntriesAreNotMine() {
        val e = TranscriptEntry("u6.0", "user", text = "status?", at = 1000, origin = "watch")
        val feed = ChatFeed.merge(listOf(e), listOf(Sent("c", "kb", "status?", sentAt = 999) to ChatRules.Status.DELIVERED))
        assertTrue(feed.any { it is ChatFeed.Item.User })
    }

    @Test fun failedMessagesDoNotMatch() {
        val e = TranscriptEntry("u7.0", "user", text = "deploy", at = 1000, origin = "phone")
        val feed = ChatFeed.merge(listOf(e), listOf(Sent("d", "kb", "deploy", sentAt = 999, failed = "x") to ChatRules.Status.FAILED))
        assertTrue(feed.any { it is ChatFeed.Item.User })
    }

    // Con pagine più vecchie ancora da caricare, i messaggi più vecchi della pagina non si accumulano in testa.
    @Test fun leftoversOlderThanThePageHideWhileMore() {
        val e = TranscriptEntry("u8.0", "user", text = "now", at = 1000, origin = "pc")
        val feed = ChatFeed.merge(listOf(e), listOf(Sent("e", "kb", "long ago", sentAt = 10) to ChatRules.Status.DONE), more = true)
        assertTrue(feed.none { it is ChatFeed.Item.Mine })
    }

    // Contratto 1.22 aggiornato (30/09 22:37): un messaggio scritto a turno in corso nasce «in coda» e poi la stessa voce
    // diventa normale; la chat la sostituisce, senza doppioni.
    @Test fun queuedEntryUpdatesInPlace() {
        val q = first.entries.single { it.queued }
        val page = TranscriptPage(listOf(q.copy(queued = false)), more = false)
        val merged = ChatFeed.append(first.entries, page, ChatFeed.Page.AFTER)
        assertEquals(first.entries.size, merged.size)
        assertFalse(merged.single { it.id == q.id }.queued)
    }

    // La lettura riparte da prima della prima voce in coda, così la vede cambiare; senza code, dalla penultima (il costo
    // del turno arriva sull'ultima dopo).
    @Test fun readsAnchorBeforeTheFirstQueued() {
        val firstQueued = first.entries.indexOfFirst { it.queued }
        assertEquals(first.entries[firstQueued - 1].id, ChatFeed.anchor(first.entries))
        val settled = first.entries.map { it.copy(queued = false) }
        assertEquals(settled[settled.size - 2].id, ChatFeed.anchor(settled))
        assertNull(ChatFeed.anchor(emptyList()))
    }

    // Se la voce è nella trascrizione il messaggio è arrivato, qualunque cosa dica il risultato che non è venuto.
    @Test fun matchedEntryProvesDelivery() {
        val e = TranscriptEntry("p9.0", "user", text = "check the logs", at = 1000, origin = "phone")
        val mine = ChatFeed.merge(listOf(e), listOf(Sent("f", "kb", "check the logs", sentAt = 990) to ChatRules.Status.UNCERTAIN))
            .filterIsInstance<ChatFeed.Item.Mine>().single()
        assertEquals(ChatRules.Status.DELIVERED, mine.status)
    }

    /** Un programmato non ancora partito non si aggancia a una voce con lo stesso testo: resta «parte alle …». */
    @Test fun scheduledIsNeverMatched() {
        val p1 = first.entries.first { it.id == "p1.0" }
        val sent = Sent("c1", "field-notes", "Also add the attendees list", sentAt = p1.at!! - 2, scheduledFor = p1.at!! + 3600)
        val feed = ChatFeed.merge(first.entries, listOf(sent to ChatRules.Status.SCHEDULED))
        assertEquals(ChatRules.Status.SCHEDULED, feed.filterIsInstance<ChatFeed.Item.Mine>().single().status)
        assertNull(feed.filterIsInstance<ChatFeed.Item.Mine>().single().entry)
    }

    // Franz, 01/10 15:59 («Gruppi + righe ricche»): i passaggi fra due messaggi di Claude diventano un gruppo.
    private fun tool(id: String, name: String, text: String, note: String? = null) =
        ChatFeed.Item.Tool(TranscriptEntry(id = id, role = "tool", tool = name, text = text, note = note, at = 1))
    private fun claude(id: String) = ChatFeed.Item.Claude(TranscriptEntry(id = id, role = "assistant", text = "ok", at = 1))

    @Test fun consecutiveToolsBecomeOneGroup() {
        val out = ChatFeed.group(listOf(claude("c1"), tool("t1", "Bash", "ls"), tool("t2", "Read", "a.md"), tool("t3", "Bash", "pwd"), claude("c2")))
        assertEquals(3, out.size)
        val steps = out[1] as ChatFeed.Item.Steps
        assertEquals(listOf("t1", "t2", "t3"), steps.entries.map { it.id })
    }

    @Test fun singleToolStaysARow() {
        val out = ChatFeed.group(listOf(claude("c1"), tool("t1", "Bash", "ls"), claude("c2")))
        assertTrue(out[1] is ChatFeed.Item.Tool)
    }

    @Test fun stepsCountByToolMostFirst() {
        val steps = ChatFeed.group(listOf(tool("t1", "Read", "a"), tool("t2", "Bash", "b"), tool("t3", "Bash", "c"))).single() as ChatFeed.Item.Steps
        assertEquals(listOf("Bash" to 2, "Read" to 1), steps.counts)
    }

    @Test fun stepsCountMcpToolsByShortName() {
        val steps = ChatFeed.group(listOf(tool("t1", "mcp__chrome-bridge__click", ""), tool("t2", "mcp__chrome-bridge__click", ""))).single() as ChatFeed.Item.Steps
        assertEquals(listOf("click" to 2), steps.counts)
    }

    // Segnalazione 02/10: una sessione appena rilanciata pulita ha la conversazione vuota; la rotella finiva mai.
    @Test fun spinnerOnlyUntilTheFirstAnswer() {
        assertTrue(ChatFeed.loading(emptyList(), answered = false))
        assertFalse(ChatFeed.loading(emptyList(), answered = true))
        assertFalse(ChatFeed.loading(first.entries, answered = false))
    }
}
