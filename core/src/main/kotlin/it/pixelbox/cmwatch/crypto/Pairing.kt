package it.pixelbox.cmwatch.crypto

import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.XECPublicKey
import java.security.spec.NamedParameterSpec
import java.security.spec.XECPrivateKeySpec
import java.security.spec.X509EncodedKeySpec
import java.security.spec.XECPublicKeySpec
import java.util.Base64
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pairing con il PC: X25519 (JCA "XDH", Android 33+ e JDK 17), chiave di sessione = HKDF-SHA256(shared,
 * info "claude-master-relay-v1", salt vuoto, 32 byte); chiavi pubbliche grezze (32 byte little-endian) in base64,
 * come `public_bytes(Raw)` di `cryptography` in cm-relay-crypto.py.
 */
object Pairing {
    private const val INFO = "claude-master-relay-v1"

    /** SubjectPublicKeyInfo DER di X25519 (RFC 8410): prefisso fisso + 32 byte grezzi. Vale su JVM e su Android (Conscrypt). */
    private val SPKI_PREFIX = byteArrayOf(0x30, 0x2a, 0x30, 0x05, 0x06, 0x03, 0x2b, 0x65, 0x6e, 0x03, 0x21, 0x00)

    /** Su Android Conscrypt non accetta NamedParameterSpec: senza parametri il default è X25519. */
    fun newKeyPair(): KeyPair {
        val g = KeyPairGenerator.getInstance("XDH")
        runCatching { g.initialize(NamedParameterSpec.X25519) }
        return g.generateKeyPair()
    }

    fun publicB64(kp: KeyPair): String {
        val enc = kp.public.encoded
        val raw = if (enc != null && enc.size >= 32) enc.copyOfRange(enc.size - 32, enc.size) else uToRaw((kp.public as XECPublicKey).u)
        return publicB64FromRaw(raw)
    }
    /** Chiave privata da scalare grezzo (32 byte): per i vettori di prova condivisi con il relay. */
    fun privateFromRaw(raw: ByteArray): PrivateKey =
        KeyFactory.getInstance("XDH").generatePrivate(XECPrivateKeySpec(NamedParameterSpec.X25519, raw))

    fun publicB64FromRaw(raw: ByteArray): String = Base64.getEncoder().encodeToString(raw)
    fun rawFromB64(b64: String): ByteArray = Base64.getDecoder().decode(b64)

    /** `info` separa gli usi: il relay (default) e il passaggio telefono → orologio (`Handoff.INFO`). */
    fun sharedKey(priv: PrivateKey, peerPubB64: String, info: String = INFO): ByteArray {
        val ka = KeyAgreement.getInstance("XDH").apply { init(priv); doPhase(publicFromRaw(rawFromB64(peerPubB64)), true) }
        return hkdf(ka.generateSecret(), info.toByteArray(), 32)
    }

    /** Chiave pubblica X25519 da 32 byte grezzi; su Android Conscrypt non sempre accetta lo SPKI, c'è il ripiego. */
    fun publicFromRaw(raw: ByteArray): PublicKey {
        require(raw.size == 32) { "peer public key must be 32 bytes" }
        val kf = KeyFactory.getInstance("XDH")
        return runCatching { kf.generatePublic(X509EncodedKeySpec(SPKI_PREFIX + raw)) }
            .getOrElse { kf.generatePublic(XECPublicKeySpec(NamedParameterSpec.X25519, BigInteger(1, raw.reversedArray()))) }
    }

    fun checkCode(key: ByteArray, code: String): String =
        hmac(key, code.toByteArray()).joinToString("") { "%02x".format(it) }.substring(0, 16)

    private fun uToRaw(u: BigInteger): ByteArray {
        val be = u.toByteArray().let { if (it.size > 32 && it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        val out = ByteArray(32)
        be.copyInto(out, 32 - be.size)
        return out.reversedArray()
    }

    private fun hmac(key: ByteArray, data: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }.doFinal(data)

    /** HKDF-SHA256 (RFC 5869), salt vuoto. */
    private fun hkdf(ikm: ByteArray, info: ByteArray, len: Int): ByteArray {
        val prk = hmac(ByteArray(32), ikm)
        var t = ByteArray(0)
        val out = ArrayList<Byte>(len)
        var i = 1
        while (out.size < len) {
            t = hmac(prk, t + info + byteArrayOf(i.toByte()))
            out.addAll(t.toList()); i++
        }
        return out.take(len).toByteArray()
    }
}
