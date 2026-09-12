package it.pixelbox.cmwatch.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * La chiave di sessione (AES-256, dal pairing) non vive mai in chiaro su disco: viene avvolta con una chiave
 * del Keystore Android (alias cmwatch-kek, AES-GCM) e il risultato base64 sta nelle preferenze.
 */
object KeyVault {
    private const val ALIAS = "cmwatch-kek"
    private const val NONCE = 12
    private val rnd = SecureRandom()

    /** Parte pura, testabile sulla JVM: base64(nonce ‖ AES-GCM(key)). */
    fun wrap(key: ByteArray, kek: SecretKey): String {
        val nonce = ByteArray(NONCE).also { rnd.nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, kek, GCMParameterSpec(128, nonce))
        return Base64.getEncoder().encodeToString(nonce + c.doFinal(key))
    }

    fun unwrap(b64: String, kek: SecretKey): ByteArray {
        return try {
            val raw = Base64.getDecoder().decode(b64)
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(128, raw, 0, NONCE))
            c.doFinal(raw, NONCE, raw.size - NONCE)
        } catch (e: Exception) { throw BlobException("cannot unwrap", e) }
    }

    /** Chiave del Keystore (creata alla prima chiamata). Solo su Android. */
    fun keystoreKek(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(false)
                .build()
        )
        return gen.generateKey()
    }

    fun newSessionKey(): ByteArray = ByteArray(32).also { rnd.nextBytes(it) }
}
