package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.rules.Badge

/** Il badge della sessione (Franz, 12/09 16:27): forma = account, riempimento = colore della sessione, glifo di stato dentro. */
@Composable
fun SessionBadge(s: Session, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val spec = Badge.of(s.account, s.color, s.state, s.icon)
    Canvas(modifier.size(size)) { drawBadge(spec, this.size.minDimension) }
}

fun DrawScope.drawBadge(spec: Badge.Spec, d: Float) {
    val fill = Color(spec.fill); val ink = Color(spec.glyphColor)
    when (spec.shape) {
        Badge.Shape.CIRCLE -> drawCircle(fill, radius = d / 2f, center = Offset(d / 2f, d / 2f))
        Badge.Shape.SQUARE -> drawRoundRect(fill, size = Size(d, d), cornerRadius = CornerRadius(d * 0.23f))
    }
    val c = Offset(d / 2f, d / 2f); val r = d * 0.26f; val w = d * 0.11f
    when (spec.glyph) {
        Badge.Glyph.PLAY -> drawPath(Path().apply {
            moveTo(c.x - r * 0.8f, c.y - r); lineTo(c.x + r, c.y); lineTo(c.x - r * 0.8f, c.y + r); close()
        }, ink)
        Badge.Glyph.CHECK -> drawPath(Path().apply {
            moveTo(c.x - r, c.y); lineTo(c.x - r * 0.25f, c.y + r * 0.75f); lineTo(c.x + r, c.y - r * 0.8f)
        }, ink, style = Stroke(width = w, cap = StrokeCap.Round))
        Badge.Glyph.CROSS -> {
            drawLine(ink, Offset(c.x - r, c.y - r), Offset(c.x + r, c.y + r), strokeWidth = w, cap = StrokeCap.Round)
            drawLine(ink, Offset(c.x + r, c.y - r), Offset(c.x - r, c.y + r), strokeWidth = w, cap = StrokeCap.Round)
        }
        Badge.Glyph.QUESTION -> {
            drawPath(Path().apply {
                moveTo(c.x - r * 0.7f, c.y - r * 0.45f)
                cubicTo(c.x - r * 0.7f, c.y - r * 1.4f, c.x + r * 0.75f, c.y - r * 1.4f, c.x + r * 0.75f, c.y - r * 0.45f)
                cubicTo(c.x + r * 0.75f, c.y + r * 0.1f, c.x, c.y + r * 0.05f, c.x, c.y + r * 0.55f)
            }, ink, style = Stroke(width = w, cap = StrokeCap.Round))
            drawCircle(ink, radius = w * 0.75f, center = Offset(c.x, c.y + r * 1.05f))
        }
    }
}
