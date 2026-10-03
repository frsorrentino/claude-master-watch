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
            color = if (mono) 0xFF000000.toInt() else spec.glyphColor
            style = Paint.Style.STROKE; strokeWidth = 3f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        // Lo stesso tracciato del badge (`Badge.paths`, icone uniche del 03/10), a tratto in 0,6 del lato.
        val box = d * 0.6f; val o = (d - box) / 2f
        c.save(); c.translate(o, o); c.scale(box / 24f, box / 24f)
        Badge.paths(spec.glyph).forEach { p -> c.drawPath(androidx.core.graphics.PathParser.createPathFromPathData(p), ink) }
        c.restore()
        return bmp
    }
}
