package it.pixelbox.cmwatch.rules

/**
 * Quali file della chat si scaricano da soli per l'anteprima: le immagini fino a 10 MB (Franz, 10/10 09:54: «vorrei che
 * nascessero già in anteprima») e i video fino a 25 MB solo su una rete senza contatore, come il Wi-Fi di casa (Franz,
 * 10/10 15:24: «Ok anteprime video»). Il resto lo chiede il tocco.
 */
object MediaPreview {
    const val IMAGE_AUTO_MAX = 10_000_000L
    /** Il tetto dei file a pezzi dal PC (contratto 1.34): oltre, il PC li rifiuta comunque. */
    const val VIDEO_AUTO_MAX = 26_214_400L

    fun auto(mime: String?, size: Long?, unmetered: Boolean): Boolean = when {
        mime == null -> false
        mime.startsWith("image/") -> (size ?: 0L) <= IMAGE_AUTO_MAX
        isVideo(mime) -> unmetered && size != null && size <= VIDEO_AUTO_MAX
        else -> false
    }

    fun isVideo(mime: String?): Boolean = mime?.startsWith("video/") == true
}
