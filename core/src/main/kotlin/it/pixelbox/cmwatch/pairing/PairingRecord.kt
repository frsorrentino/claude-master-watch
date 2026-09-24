package it.pixelbox.cmwatch.pairing

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Solo telefono: con chi è accoppiato e se all'orologio manca ancora K (design 24/09, «Casi particolari»). */
@Serializable
data class PairingRecord(
    val uids: List<String>,
    val names: Map<String, String>,
    val watchUid: String? = null,
    val watchName: String? = null,
    val watchPending: Boolean = false,
) {
    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun fromJson(s: String?): PairingRecord? = s?.let { runCatching { json.decodeFromString(serializer(), it) }.getOrNull() }
    }
}
