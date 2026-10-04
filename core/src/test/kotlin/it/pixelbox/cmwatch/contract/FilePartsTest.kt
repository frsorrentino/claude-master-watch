package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.crypto.Blob
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Contratto 1.34 (Franz, 04/10 21:55: un HTML di 3,6 MB non arrivava): il vettore del relay, decifrato dall'app. */
class FilePartsTest {
    private val root = Json.parseToJsonElement(Fixtures.read("file-parts.json")).jsonObject
    private val key = root.getValue("key").jsonPrimitive.content.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test fun theMetaOpensToTheManifest() {
        val meta = ContractJson.json.decodeFromString(FileMeta.serializer(), Blob.open(root.getValue("meta").toString(), key))
        assertEquals(ContractJson.json.decodeFromJsonElement(FileMeta.serializer(), root.getValue("meta_plain")), meta)
        assertEquals(2, meta.n)
    }

    @Test fun thePartsGiveBackTheFileWithItsHash() {
        val bytes = root.getValue("parts").jsonArray.map { Blob.openBytes(it.toString(), key) }.reduce { a, b -> a + b }
        assertArrayEquals(root.getValue("file").jsonPrimitive.content.toByteArray(), bytes)
        val hex = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        assertEquals(root.getValue("meta_plain").jsonObject.getValue("sha256").jsonPrimitive.content, hex)
    }
}
