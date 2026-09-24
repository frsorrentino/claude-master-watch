package it.pixelbox.cmwatch.mobile.pair

import it.pixelbox.cmwatch.crypto.KeyVault

object KeystoreKeyWrap : KeyWrap {
    override fun wrap(key: ByteArray): String = KeyVault.wrap(key, KeyVault.keystoreKek())
    override fun unwrap(wrapped: String): ByteArray = KeyVault.unwrap(wrapped, KeyVault.keystoreKek())
}
