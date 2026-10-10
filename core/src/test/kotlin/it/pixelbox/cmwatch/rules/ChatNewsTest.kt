package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatNewsTest {
    private fun claude(id: String, at: Long) = ChatFeed.Item.Claude(TranscriptEntry(id, "assistant", "ok", at))
    private fun user(id: String, at: Long) = ChatFeed.Item.User(TranscriptEntry(id, "user", "go", at))
    private fun mine(id: String, at: Long) = ChatFeed.Item.Mine(Sent(id, "x", "fatto?", at), ChatRules.Status.DELIVERED, null)
    private fun steps(vararg at: Long) = ChatFeed.Item.Steps(at.mapIndexed { i, t -> TranscriptEntry("s$i", "tool", "Bash", t) })

    private val feed = listOf(claude("a", 100), user("b", 200), mine("c", 300), steps(310, 320), claude("d", 330))

    @Test fun theNewOnesStartAfterTheLastVisitAndSkipYourOwn() {
        assertEquals(3, ChatNews.firstNew(feed, seenAt = 250))
        assertEquals(1, ChatNews.firstNew(feed, seenAt = 150))
        assertNull(ChatNews.firstNew(feed, seenAt = 330))
    }

    /** La prima volta che una sessione si apre tutto sarebbe nuovo: niente riga «Nuovi». */
    @Test fun aSessionNeverOpenedHasNothingNew() {
        assertNull(ChatNews.firstNew(feed, seenAt = null))
    }

    @Test fun theLatestMomentIsRememberedWhenLeaving() {
        assertEquals(330L, ChatNews.latest(feed))
        assertNull(ChatNews.latest(emptyList()))
        assertEquals(320L, ChatNews.at(steps(310, 320)))
    }

    @Test fun onlyTheLastSixEnterInCascadeFromTheTop() {
        assertEquals(listOf(null, null, null, null, 0, 1, 2, 3, 4, 5), (0 until 10).map { ChatNews.rank(it, 10) })
        assertEquals(listOf(0, 1, 2), (0 until 3).map { ChatNews.rank(it, 3) })
    }
}
