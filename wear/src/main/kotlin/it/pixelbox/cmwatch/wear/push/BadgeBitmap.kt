package it.pixelbox.cmwatch.wear.push

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import it.pixelbox.cmwatch.rules.Badge

/** Lo stesso badge come bitmap: largeIcon delle notifiche, Person delle conversazioni, complication monocroma. */
object BadgeBitmap {
    fun draw(spec: Badge.Spec, sizePx: Int = 96, mono: Boolean = false): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp); val d = sizePx.toFloat()
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (mono) 0xFFFFFFFF.toInt() else spec.fill }
        when (spec.shape) {
            Badge.Shape.CIRCLE -> c.drawCircle(d / 2, d / 2, d / 2, fill)
            Badge.Shape.SQUARE -> c.drawRoundRect(RectF(0f, 0f, d, d), d * 0.23f, d * 0.23f, fill)
        }
        val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (mono) 0xFF000000.toInt() else spec.glyphColor; strokeWidth = d * 0.11f; strokeCap = Paint.Cap.ROUND; style = Paint.Style.STROKE
        }
        val cx = d / 2; val cy = d / 2; val r = d * 0.26f
        when (spec.glyph) {
            Badge.Glyph.PLAY -> c.drawPath(Path().apply { moveTo(cx - r * 0.8f, cy - r); lineTo(cx + r, cy); lineTo(cx - r * 0.8f, cy + r); close() }, Paint(ink).apply { style = Paint.Style.FILL })
            Badge.Glyph.CHECK -> c.drawPath(Path().apply { moveTo(cx - r, cy); lineTo(cx - r * 0.25f, cy + r * 0.75f); lineTo(cx + r, cy - r * 0.8f) }, ink)
            Badge.Glyph.CROSS -> { c.drawLine(cx - r, cy - r, cx + r, cy + r, ink); c.drawLine(cx + r, cy - r, cx - r, cy + r, ink) }
            Badge.Glyph.QUESTION -> {
                c.drawPath(Path().apply {
                    moveTo(cx - r * 0.7f, cy - r * 0.45f)
                    cubicTo(cx - r * 0.7f, cy - r * 1.4f, cx + r * 0.75f, cy - r * 1.4f, cx + r * 0.75f, cy - r * 0.45f)
                    cubicTo(cx + r * 0.75f, cy + r * 0.1f, cx, cy + r * 0.05f, cx, cy + r * 0.55f)
                }, ink)
                c.drawCircle(cx, cy + r * 1.05f, ink.strokeWidth * 0.75f, Paint(ink).apply { style = Paint.Style.FILL })
            }
        }
        return bmp
    }
}
