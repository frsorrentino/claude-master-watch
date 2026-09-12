package it.pixelbox.cmwatch.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

class KeyVaultTest {
    private fun kek() = SecretKeySpec(ByteArray(32).also { SecureRandom().nextBytes(it) }, "AES")
    private val key = ByteArray(32).also { SecureRandom().nextBytes(it) }

    @Test fun wrapThenUnwrap() {
        val k = kek()
        val wrapped = KeyVault.wrap(key, k)
        assertArrayEquals(key, KeyVault.unwrap(wrapped, k))
        assertNotEquals(wrapped, KeyVault.wrap(key, k))   // nonce diverso
    }

    @Test(expected = BlobException::class) fun otherKekFails() {
        KeyVault.unwrap(KeyVault.wrap(key, kek()), kek())
    }
}
