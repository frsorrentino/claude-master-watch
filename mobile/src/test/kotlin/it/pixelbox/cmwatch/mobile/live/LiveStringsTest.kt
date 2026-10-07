package it.pixelbox.cmwatch.mobile.live

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Le frasi della live esistono in italiano e in inglese, con gli stessi segnaposto, e senza «…» (regole del 12/09). */
class LiveStringsTest {
    private fun live(dir: String): Map<String, String> =
        Regex("""<string name="(live_[a-z_]+)">(.*?)</string>""").findAll(File("src/main/res/$dir/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private fun holes(s: String) = Regex("%\\d\\$[sd]").findAll(s).map { it.value }.sorted().toList()

    @Test fun italianAndEnglishHaveTheSameKeysAndPlaceholders() {
        val it = live("values")
        val en = live("values-en")
        assertTrue(it.size > 40)
        assertEquals(it.keys, en.keys)
        for ((k, v) in it) assertEquals(k, holes(v), holes(en.getValue(k)))
        for (v in it.values + en.values) assertFalse(v, "…" in v)
    }
}
