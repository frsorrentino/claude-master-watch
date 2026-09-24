package it.pixelbox.cmwatch.pairing

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Richieste e risposte fra telefono e orologio sul canale di Wear OS (design 24/09, «Messaggi fra telefono e orologio»). */
@Serializable data class HelloRequest(val v: Int = 1, val f: QrFirebase)

@Serializable data class HelloResponse(
    val v: Int = 1, val uid: String? = null, val name: String? = null, val eph: String? = null,
    val restart: Boolean = false, val error: String? = null,
)

@Serializable data class KeyRequest(val v: Int = 1, val host: String, val eph: String, val box: String)

@Serializable data class KeyResponse(val v: Int = 1, val ok: Boolean = false, val error: String? = null)

object HandoffMessages {
    const val HELLO = "/cmwatch/pair/hello"
    const val KEY = "/cmwatch/pair/key"
    const val ERR_AUTH = "auth"
    const val ERR_NO_SESSION = "no_session"
    const val ERR_DECRYPT = "decrypt"
    const val ERR_STORE = "store"
    const val ERR_BAD_REQUEST = "bad_request"

    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    fun <T> encode(s: KSerializer<T>, v: T): ByteArray = json.encodeToString(s, v).toByteArray()
    fun <T> decode(s: KSerializer<T>, b: ByteArray): T? = runCatching { json.decodeFromString(s, String(b)) }.getOrNull()
}
