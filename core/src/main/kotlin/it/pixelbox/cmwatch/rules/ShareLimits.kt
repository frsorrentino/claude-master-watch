package it.pixelbox.cmwatch.rules

/**
 * Quanto può pesare un file da allegare, dato `state.share.max_bytes` (contratto 1.19: la lunghezza massima di `enc`).
 * La busta codifica il file due volte in base64: i dati in chiaro `{mime, data, name}`, poi il cifrato. Pesa circa 16/9
 * del file, non 4/3: col conto di prima un file da 1 MB passava il controllo e il caricamento lo rifiutava (Franz, 07/10
 * 19:49).
 */
object ShareLimits {
    /** Nonce (12 byte) e tag (16 byte) di AES-GCM. */
    private const val GCM_BYTES = 28
    /** Le chiavi e le virgolette del JSON, `mime` e un nome fino a 120 caratteri anche non ASCII (4 byte l'uno in UTF-8). */
    private const val JSON_BYTES = 1024

    fun maxFileBytes(maxEnc: Int): Long {
        if (maxEnc <= 0) return 0
        val plain = 3L * (maxEnc / 4) - GCM_BYTES - JSON_BYTES
        return (3L * (plain / 4)).coerceAtLeast(0)
    }
}
