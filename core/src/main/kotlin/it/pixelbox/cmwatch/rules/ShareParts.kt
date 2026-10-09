package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.FileMeta

/**
 * Contratto 1.48 (Franz, 09/10 19:17: «imposta limite alto se possibile, es. 50mb»): un file dal dispositivo a pezzi,
 * /share/<id>/parts/0..n-1 e per ultimo /share/<id>/meta, come /file della 1.34 nell'altro verso.
 */
object ShareParts {
    /** Il pezzo consigliato dal relay: 1 MiB prima della cifratura. */
    const val PART_BYTES = 1_048_576
    /** Quanti pezzi il relay accetta. */
    const val MAX_PARTS = 50

    fun split(data: ByteArray, partBytes: Int = PART_BYTES): List<ByteArray> =
        if (data.isEmpty()) listOf(data) else (data.indices step partBytes).map { data.copyOfRange(it, minOf(it + partBytes, data.size)) }

    /** Il manifesto in chiaro di `meta`: quanti pezzi, la misura, lo sha256 in esadecimale, il tipo e il nome. */
    fun meta(data: ByteArray, n: Int, mime: String, name: String?): FileMeta = FileMeta(
        n, data.size.toLong(),
        java.security.MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }, mime, name,
    )
}
