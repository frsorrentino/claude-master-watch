package it.pixelbox.cmwatch.crypto

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class BlobException(msg: String, cause: Throwable? = null) : Exception(msg, cause)

/**
 * Documento sul bus: {"v":1,"enc":"<base64>"}, enc = nonce (12 byte) ‖ AES-256-GCM(ciphertext ‖ tag),
 * AAD "claude-master-relay-v1". Stesso layout di cm-relay-crypto.py nel plugin claude-master.
 */
object Blob {
    const val VERSION = 1
    val AAD: ByteArray = "claude-master-relay-v1".toByteArray()
    private const val NONCE = 12
    private val rnd = SecureRandom()

    fun seal(plain: String, key: ByteArray): String {
        val nonce = ByteArray(NONCE).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        c.updateAAD(AAD)
        val ct = c.doFinal(plain.toByteArray())
        val enc = Base64.getEncoder().encodeToString(nonce + ct)
        return Json.encodeToString(JsonObject.serializer(), JsonObject(mapOf("v" to JsonPrimitive(VERSION), "enc" to JsonPrimitive(enc))))
    }

    fun open(doc: String, key: ByteArray): String {
        val o = runCatching { Json.parseToJsonElement(doc).jsonObject }.getOrElse { throw BlobException("not a document", it) }
        if (o["v"]?.jsonPrimitive?.content?.toIntOrNull() != VERSION) throw BlobException("unsupported version")
        val raw = runCatching { Base64.getDecoder().decode(o.getValue("enc").jsonPrimitive.content) }
            .getOrElse { throw BlobException("bad base64", it) }
        if (raw.size < NONCE + 16) throw BlobException("too short")
        return try {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, raw, 0, NONCE))
            c.updateAAD(AAD)
            String(c.doFinal(raw, NONCE, raw.size - NONCE))
        } catch (e: Exception) { throw BlobException("cannot open", e) }
    }
}
