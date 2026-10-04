package it.pixelbox.cmwatch.mobile.ui.art

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import it.pixelbox.cmwatch.mobile.ui.CmMotion
import it.pixelbox.cmwatch.mobile.ui.animationsOff
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Le scene contestuali (Franz, 25/09 06:58): vettori in Compose con i CmColors, una volta all'ingresso; con le animazioni
 * spente, e negli snapshot, lo stato finale subito.
 */
@Composable
private fun entrance(): Float {
    val still = animationsOff() || LocalInspectionMode.current
    val a = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(Unit) { if (!still) a.animateTo(1f, CmMotion.spec(false)) }
    return a.value
}

private fun DrawScope.pc(c: Offset, w: Float, color: Color = CmColors.surfaceHigh) {
    drawRoundRect(color, Offset(c.x - w / 2, c.y - w * 0.32f), Size(w, w * 0.62f), CornerRadius(w * 0.05f))
    drawRect(CmColors.line, Offset(c.x - w * 0.08f, c.y + w * 0.3f), Size(w * 0.16f, w * 0.1f))
}

private fun DrawScope.phone(c: Offset, h: Float, color: Color = CmColors.surfaceHigh) =
    drawRoundRect(color, Offset(c.x - h * 0.25f, c.y - h / 2), Size(h * 0.5f, h), CornerRadius(h * 0.08f))

private fun DrawScope.watch(c: Offset, r: Float, color: Color = CmColors.surfaceHigh) {
    drawCircle(color, r, c)
    drawCircle(CmColors.line, r, c, style = Stroke(r * 0.12f))
}

private fun DrawScope.qr(topLeft: Offset, cell: Float, alpha: Float) {
    for (y in 0 until 7) for (x in 0 until 7) {
        val on = (x + y * 3) % 4 != 0 || x == 0 || y == 0 || x == 6 || y == 6
        if (on) drawRect(CmColors.text.copy(alpha = 0.85f * alpha), Offset(topLeft.x + x * cell, topLeft.y + y * cell), Size(cell * 0.9f, cell * 0.9f))
    }
}

@Composable
fun ScanScene(modifier: Modifier = Modifier) {
    val t = entrance()
    Canvas(modifier) {
        val c = Offset(size.width * 0.42f, size.height * 0.5f)
        pc(c, size.minDimension * 0.9f)
        val cell = size.minDimension * 0.045f
        qr(Offset(c.x - cell * 3.5f, c.y - cell * 4.5f), cell, t)
        phone(Offset(size.width * 0.78f, size.height * 0.55f + (1 - t) * 24f), size.minDimension * 0.55f, CmColors.surface)
        drawLine(CmColors.primary.copy(alpha = t), Offset(size.width * 0.66f, size.height * 0.5f), Offset(c.x + cell * 4f, c.y - cell), strokeWidth = 4f)
    }
}

@Composable
fun PairedScene(watch: Boolean, modifier: Modifier = Modifier) {
    val t = entrance()
    Canvas(modifier) {
        val m = Offset(size.width * 0.5f, size.height * 0.45f)
        val p = Offset(size.width * 0.2f, size.height * 0.6f)
        val w = Offset(size.width * 0.8f, size.height * 0.6f)
        drawLine(CmColors.idle.copy(alpha = t), p, m, strokeWidth = 5f)
        if (watch) drawLine(CmColors.idle.copy(alpha = t), w, m, strokeWidth = 5f)
        pc(m, size.minDimension * 0.7f)
        phone(p, size.minDimension * 0.55f, CmColors.surface)
        if (watch) watch(w, size.minDimension * 0.16f, CmColors.surface)
    }
}

@Composable
fun EmptySessionsScene(modifier: Modifier = Modifier) {
    val t = entrance()
    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        pc(c, size.minDimension * 0.9f)
        drawCircle(CmColors.stale.copy(alpha = 0.6f * t), size.minDimension * 0.05f, c)
    }
}

@Composable
fun EmptyDiaryScene(modifier: Modifier = Modifier) {
    val t = entrance()
    Canvas(modifier) {
        val w = size.minDimension * 0.55f
        val tl = Offset(size.width / 2 - w / 2, size.height / 2 - w * 0.65f)
        drawRoundRect(CmColors.surfaceHigh, tl, Size(w, w * 1.3f), CornerRadius(w * 0.06f))
        for (i in 0 until 4) {
            val y = tl.y + w * (0.25f + i * 0.22f)
            drawLine(CmColors.line.copy(alpha = t), Offset(tl.x + w * 0.15f, y), Offset(tl.x + w * (0.85f - i * 0.1f), y), strokeWidth = 6f)
        }
        drawCircle(CmColors.waiting.copy(alpha = t), w * 0.09f, Offset(tl.x + w * 0.85f, tl.y + w * 1.15f))
    }
}
