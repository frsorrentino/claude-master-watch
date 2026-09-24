package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import java.security.PrivateKey
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class HandoffException(val code: String, cause: Throwable? = null) : Exception(code, cause)

/**
 * K dal telefono all'orologio (design 24/09, «Sicurezza»): kw = HKDF-SHA256(X25519(eph del telefono, eph dell'orologio),
 * info "cmwatch-handoff-v1"); box = base64(nonce di 12 byte ‖ AES-256-GCM(kw, K, aad = uid dell'orologio)). Il canale di
 * Wear OS può passare dal cloud Google: così anche lì viaggiano solo dati cifrati.
 */
object Handoff {
    const val INFO = "cmwatch-handoff-v1"
    private const val NONCE = 12
    private val rnd = SecureRandom()

    fun seal(
        key: ByteArray, phoneEph: PrivateKey, watchEphPubB64: String, watchUid: String,
        nonce: ByteArray = ByteArray(NONCE).also { rnd.nextBytes(it) },
    ): String {
        val c = cipher(Cipher.ENCRYPT_MODE, Pairing.sharedKey(phoneEph, watchEphPubB64, INFO), nonce, watchUid)
        return Base64.getEncoder().encodeToString(nonce + c.doFinal(key))
    }

    fun open(box: String, watchEph: PrivateKey, phoneEphPubB64: String, watchUid: String): ByteArray = try {
        val raw = Base64.getDecoder().decode(box)
        val c = cipher(Cipher.DECRYPT_MODE, Pairing.sharedKey(watchEph, phoneEphPubB64, INFO), raw.copyOfRange(0, NONCE), watchUid)
        c.doFinal(raw, NONCE, raw.size - NONCE).also { require(it.size == 32) { "key must be 32 bytes" } }
    } catch (e: Exception) {
        throw HandoffException(HandoffMessages.ERR_DECRYPT, e)
    }

    private fun cipher(mode: Int, kw: ByteArray, nonce: ByteArray, aad: String): Cipher =
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, SecretKeySpec(kw, "AES"), GCMParameterSpec(128, nonce))
            updateAAD(aad.toByteArray())
        }
}
