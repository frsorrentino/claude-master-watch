package it.pixelbox.cmwatch.pairing

import java.net.URI
import java.util.Base64

/**
 * L'invito a zero tocchi (Franz, 04/10 15:48, «ok c»): sul Chromebook il relay apre nell'Android della stessa macchina
 * `cmwatch://pair?q=<base64url della riga di relay pair --text>`. Il link è esportato e apribile da una pagina web: qui
 * si legge e si controlla soltanto, l'app chiede sempre conferma (con il nome del PC) prima di accoppiare.
 */
object PairLink {
    /** La riga del QR dentro il link, se è un invito di claude-master valido; null altrimenti (anche senza `q`). */
    fun line(uri: String): String? {
        val u = runCatching { URI(uri) }.getOrNull() ?: return null
        if (u.scheme != "cmwatch" || u.host != "pair") return null
        val q = u.rawQuery?.split('&')?.firstOrNull { it.startsWith("q=") }?.removePrefix("q=") ?: return null
        val text = runCatching { String(Base64.getUrlDecoder().decode(q), Charsets.UTF_8) }.getOrNull() ?: return null
        return text.takeIf { PairQr.parse(it) != null }
    }
}
