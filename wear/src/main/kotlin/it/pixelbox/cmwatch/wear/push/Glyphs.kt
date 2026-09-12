package it.pixelbox.cmwatch.wear.push

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import it.pixelbox.cmwatch.contract.SessionState

/** Glifo di stato colorato (❓ ambra, ▶ blu, ✓ verde, ✗ rosso) come bitmap: icona grande e Person della conversazione. */
object Glyphs {
    fun state(ctx: Context, state: SessionState?, sizePx: Int = 96): Bitmap {
        val (color, text) = when (state) {
            SessionState.WAITING -> 0xFFFFB020.toInt() to "?"
            SessionState.BUSY, SessionState.AWAITING -> 0xFF7FA1FF.toInt() to "▶"
            SessionState.IDLE -> 0xFF34C759.toInt() to "✓"
            SessionState.GONE -> 0xFFFF453A.toInt() to "✗"
            null -> 0xFF4C7DFF.toInt() to "◔"
        }
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        c.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, p)
        p.color = Color.BLACK; p.textSize = sizePx * 0.58f; p.textAlign = Paint.Align.CENTER; p.isFakeBoldText = true
        val y = sizePx / 2f - (p.descent() + p.ascent()) / 2f
        c.drawText(text, sizePx / 2f, y, p)
        return bmp
    }
}
