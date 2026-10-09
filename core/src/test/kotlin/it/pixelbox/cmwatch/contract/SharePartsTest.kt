package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.crypto.Blob
import it.pixelbox.cmwatch.rules.ShareLimits
import it.pixelbox.cmwatch.rules.ShareParts
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contratto 1.48 (Franz, 09/10 19:17: «imposta limite alto se possibile, es. 50mb»): il file dal dispositivo a pezzi. */
class SharePartsTest {
    private val root = Json.parseToJsonElement(Fixtures.read("share-parts.json")).jsonObject
    private val key = root.getValue("key").jsonPrimitive.content.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    private val file = root.getValue("file").jsonPrimitive.content.toByteArray()
    private val partBytes = root.getValue("part_bytes").jsonPrimitive.content.toInt()

    @Test fun theSplitAndTheManifestAreTheRelaysOnes() {
        val parts = ShareParts.split(file, partBytes)
        assertEquals(3, parts.size)
        // I pezzi del relay, aperti con la chiave di prova, sono i nostri.
        val theirs = root.getValue("parts").jsonArray.map { Blob.openBytes(it.toString(), key) }
        parts.zip(theirs).forEach { (a, b) -> assertArrayEquals(a, b) }
        val meta = ShareParts.meta(file, parts.size, "text/plain", "notes.txt")
        assertEquals(ContractJson.json.decodeFromJsonElement(FileMeta.serializer(), root.getValue("meta_plain")), meta)
    }

    @Test fun ourSealedPartsOpenBackToTheFile() {
        val sealed = ShareParts.split(file, partBytes).map { Blob.sealBytes(it, key) }
        assertArrayEquals(file, sealed.map { Blob.openBytes(it, key) }.reduce { a, b -> a + b })
    }

    @Test fun theLimitWithPartsIsTheWholeFileOneAndFiftyPartsAtMost() {
        val withParts = Share(maxBytes = 10_000_000, any = true, parts = true, maxPartsBytes = 52_428_800)
        assertEquals(52_428_800L, ShareLimits.maxFileBytes(withParts))
        assertEquals(ShareLimits.maxFileBytes(10_000_000), ShareLimits.maxFileBytes(withParts.copy(parts = false)))
        assertEquals(0L, ShareLimits.maxFileBytes(null as Share?))
        // Pezzi da 1 MiB: 50 MB stanno nei 50 pezzi che il relay accetta.
        assertTrue(ShareParts.split(ByteArray(52_428_800), ShareParts.PART_BYTES).size <= ShareParts.MAX_PARTS)
        val st = ContractJson.decodeState(Fixtures.stateIdle)
        assertEquals(true, st.share?.parts); assertEquals(52_428_800L, st.share?.maxPartsBytes)
    }
}
