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
