package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Il contenuto del QR di `claude-master relay pair` (contratto 1.15, `contract/pair-qr.json`). */
@Serializable
data class PairQr(val v: Int, val i: String, val c: String, val h: String, val e: Long, val f: QrFirebase) {
    fun expired(nowSec: Long): Boolean = nowSec > e

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        private val ID = Regex("[A-Za-z0-9_-]{22}")

        /** Testo del QR o di «Incolla il codice»; null se non è un codice di claude-master (altro QR, testo tagliato, versione futura). */
        fun parse(text: String): PairQr? {
            val q = runCatching { json.decodeFromString(serializer(), text.trim()) }.getOrNull() ?: return null
            val pubOk = runCatching { Pairing.rawFromB64(q.c).size == 32 }.getOrDefault(false)
            val fbOk = listOf(q.f.k, q.f.p, q.f.a, q.f.t).none(String::isBlank) && q.f.d.startsWith("https://")
            return q.takeIf { it.v == 1 && ID.matches(it.i) && it.h.isNotBlank() && pubOk && fbOk }
        }
    }
}
