package it.pixelbox.cmwatch.wear.push

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.SessionState

/**
 * L'icona grande delle notifiche: cerchio scuro con l'icona dello stato nel suo colore. Prima era un carattere («?», «✗»)
 * nero su cerchio pieno, che sul polso usciva grossolano (Franz, 16/09 16:30). I significati restano quelli di Telegram:
 * fumetto ambra = aspetta una risposta, triangolo azzurro = lavora, spunta verde = esito, stop rosso spento = chiusa.
 */
object Glyphs {
    fun state(ctx: Context, state: SessionState?, sizePx: Int = 96): Bitmap {
        val (res, color) = when (state) {
            SessionState.WAITING -> R.drawable.ic_state_question to 0xFFFFB020.toInt()
            SessionState.BUSY, SessionState.AWAITING -> R.drawable.ic_state_busy to 0xFF8BB4F7.toInt()
            SessionState.IDLE -> R.drawable.ic_state_done to 0xFF65C581.toInt()
            SessionState.GONE -> R.drawable.ic_state_gone to 0xFFC2554D.toInt()
            null -> R.drawable.ic_app_mono to 0xFF8BB4F7.toInt()
        }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = 0xFF262A32.toInt() })
        val icon = ContextCompat.getDrawable(ctx, res)?.mutate() ?: return bmp
        icon.setTint(color)
        val pad = (sizePx * 0.22f).toInt()
        icon.setBounds(pad, pad, sizePx - pad, sizePx - pad)
        icon.draw(c)
        return bmp
    }
}
