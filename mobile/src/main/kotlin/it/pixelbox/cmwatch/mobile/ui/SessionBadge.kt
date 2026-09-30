package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Badge
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il badge della sessione come sul polso e sul desktop (Franz, 30/09 20:31): forma = account (cerchio personale, quadrato
 * lavoro), riempimento = colore della sessione, glifo di stato dentro, dalla regola `Badge` di `core`. Respira mentre la
 * sessione lavora, fermo con le animazioni spente.
 */
@Composable
fun SessionBadge(s: Session, size: Dp = 22.dp, modifier: Modifier = Modifier) {
    val spec = Badge.of(s.account, s.color, s.state, s.icon, s.accountKind)
    val alpha = if (Badge.breathes(s.state) && !animationsOff()) {
        val a by rememberInfiniteTransition(label = "respiro").animateFloat(1f, 0.55f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "a")
        a
    } else 1f
    val labels = Badge.Labels(
        waiting = stringResource(R.string.state_waiting), busy = stringResource(R.string.state_busy),
        idle = stringResource(R.string.state_idle), gone = stringResource(R.string.state_closed),
        awaiting = stringResource(R.string.state_awaiting), personal = stringResource(R.string.badge_personal),
        work = stringResource(R.string.badge_work),
    )
    val description = Badge.description(s.account, s.accountKind, s.state, labels)
    Canvas(modifier.size(size).semantics { contentDescription = description }) { drawBadge(spec, this.size.minDimension, alpha) }
}

/** Solo la forma dell'account, vuota: dove c'è l'account ma nessuna sessione (quota, «Lancia», filtro). */
@Composable
fun AccountMark(personal: Boolean, modifier: Modifier = Modifier, size: Dp = 14.dp, color: Color = CmColors.text2) {
    val description = stringResource(if (personal) R.string.badge_personal else R.string.badge_work)
    Canvas(modifier.size(size).semantics { contentDescription = description }) {
        val d = this.size.minDimension
        val stroke = Stroke(width = d * 0.14f)
        if (personal) drawCircle(color, radius = d / 2f - d * 0.07f, center = Offset(d / 2f, d / 2f), style = stroke)
        else drawRoundRect(color, topLeft = Offset(d * 0.07f, d * 0.07f), size = Size(d * 0.86f, d * 0.86f), cornerRadius = CornerRadius(d * 0.23f), style = stroke)
    }
}

/** Le stesse proporzioni del badge dell'orologio (`drawBadge` in `wear`), così polso e telefono si riconoscono. */
private fun DrawScope.drawBadge(spec: Badge.Spec, d: Float, alpha: Float) {
    val fill = Color(spec.fill).copy(alpha = alpha)
    val ink = Color(spec.glyphColor).copy(alpha = alpha)
    when (spec.shape) {
        Badge.Shape.CIRCLE -> drawCircle(fill, radius = d / 2f, center = Offset(d / 2f, d / 2f))
        Badge.Shape.SQUARE -> drawRoundRect(fill, size = Size(d, d), cornerRadius = CornerRadius(d * 0.23f))
    }
    val c = Offset(d / 2f, d / 2f); val r = d * 0.22f; val w = d * 0.095f
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
