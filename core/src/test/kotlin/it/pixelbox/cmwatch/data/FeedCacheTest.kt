package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.contract.TranscriptEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Le conversazioni già lette, tenute dal processo e su disco (piano prestazioni, Task 13): a ogni ricreazione della
 * finestra (Chromebook ridimensionato, rotazione, tema) le chat ripartivano vuote, con una lettura completa per colonna.
 */
class FeedCacheTest {
    @get:Rule val tmp = TemporaryFolder()
    private fun entries(n: Int, from: Int = 0) = (from until from + n).map { TranscriptEntry("e$it", "assistant", text = "voce $it", at = it.toLong()) }

    @Test fun aNewInstanceOnTheSameFolderFindsTheSameEntries() {
        val dir = tmp.newFolder()
        FeedCache(dir).save(mapOf("ledger-api" to entries(3), "master" to entries(2)))
        val again = FeedCache(dir).all()
        assertEquals(entries(3), again["ledger-api"]); assertEquals(entries(2), again["master"])
    }

    @Test fun onlyTheLastFiftyAreKept() {
        val dir = tmp.newFolder()
        FeedCache(dir).save(mapOf("atlas" to entries(80)))
        val kept = FeedCache(dir).all().getValue("atlas")
        assertEquals(50, kept.size); assertEquals("e30", kept.first().id); assertEquals("e79", kept.last().id)
    }

    @Test fun aSessionNameWithOddCharactersIsAFileAnyway() {
        val dir = tmp.newFolder()
        FeedCache(dir).save(mapOf("rino/fiscale: 2026" to entries(1)))
        assertEquals(entries(1), FeedCache(dir).all()["rino/fiscale: 2026"])
    }

    /**
     * Segnalazione di Franz, 08/10 08:05: le risposte lunghe finivano a 4000 caratteri. Il relay ora ne manda fino a 20000
     * (team-supervisor `c95b20b`); una conversazione salvata con una voce tagliata dal tetto vecchio si rilegge da capo.
     */
    @Test fun aConversationCutByTheOldRelayIsReadAgain() {
        val dir = tmp.newFolder()
        val old = TranscriptEntry("a1", "assistant", text = "x".repeat(4000), at = 1, cut = true)
        val new = TranscriptEntry("a2", "assistant", text = "y".repeat(19_990), at = 2, cut = true)
        FeedCache(dir).save(mapOf("rino" to entries(2) + old, "atlas" to entries(2) + new, "master" to entries(3)))
        val again = FeedCache(dir).all()
        assertTrue("rino" !in again)
        assertEquals(entries(2) + new, again["atlas"]); assertEquals(entries(3), again["master"])
    }

    @Test fun aBrokenFileIsIgnored() {
        val dir = tmp.newFolder()
        java.io.File(dir, "x.json").writeText("{ non è json")
        assertTrue(FeedCache(dir).all().isEmpty())
    }

    @Test fun anUnchangedSessionIsNotWrittenAgain() {
        val dir = tmp.newFolder()
        val c = FeedCache(dir)
        c.save(mapOf("a" to entries(2)))
        val f = dir.listFiles()!!.single()
        f.setLastModified(1_000)
        c.save(mapOf("a" to entries(2)))
        assertEquals(1_000, f.lastModified())
    }
}
