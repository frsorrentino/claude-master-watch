package it.pixelbox.cmwatch.rules

/**
 * Barra lineare della quota. ProtoLayout Material 3 1.4.2 ha solo l'indicatore circolare, quindi la barra è una riga
 * di due parti pesate — piena e traccia — con lo stacco in mezzo come negli indicatori lineari M3. Qui sta la
 * decisione dei pesi; il disegno sta nella tile.
 */
object QuotaBar {
    data class Spec(val fill: Int, val track: Int, val gap: Boolean)

    /** Sotto questa percentuale la pillolina piena sarebbe più corta del suo raggio e si vedrebbe come un puntino. */
    private const val MIN = 3

    fun of(pct: Int?): Spec {
        val p = (pct ?: 0).coerceIn(0, 100)
        return when {
            p < MIN -> Spec(fill = 0, track = 100, gap = false)
            p > 100 - MIN -> Spec(fill = 100, track = 0, gap = false)
            else -> Spec(fill = p, track = 100 - p, gap = true)
        }
    }
}
