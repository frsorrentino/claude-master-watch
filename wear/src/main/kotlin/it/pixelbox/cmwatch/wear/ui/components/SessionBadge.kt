package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import it.pixelbox.cmwatch.R
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.Badge

/** Il badge della sessione (Franz, 12/09 16:27): forma = account, riempimento = colore della sessione, glifo di stato dentro. */
@Composable
fun SessionBadge(s: Session, size: Dp = 22.dp, modifier: Modifier = Modifier, ambient: Boolean = false, animate: Boolean = true) {
    val spec = Badge.of(s.account, s.color, s.state, s.icon, s.accountKind)
    // Respiro di 3 s su ogni sessione che lavora (Franz, 14/09 16:24); fermo in ambient o con le animazioni spente.
    val breathing = animate && !ambient && Badge.breathes(s.state)
    val alpha by if (breathing) {
        rememberInfiniteTransition(label = "respiro").animateFloat(
            initialValue = 1f, targetValue = 0.55f,
            animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "alpha",
        )
    } else remember { mutableStateOf(1f) }
    // Il badge è solo disegno: a TalkBack diciamo lo stato del glifo e l'account della forma (S07).
    val labels = Badge.Labels(
        waiting = stringResource(R.string.state_waiting), busy = stringResource(R.string.state_busy),
        idle = stringResource(R.string.state_idle), gone = stringResource(R.string.state_gone),
        awaiting = stringResource(R.string.state_awaiting_prompt),
        personal = stringResource(R.string.badge_personal), work = stringResource(R.string.badge_work),
    )
    val description = Badge.description(s.account, s.accountKind, s.state, labels)
    // Il cambio di stato (▶ → ✓, ✓ → ❓) passa con dissolvenza e scala invece di scattare (A5, 15/09 23:40); in ambient no.
    AnimatedContent(
        targetState = spec,
        transitionSpec = {
            if (ambient) EnterTransition.None togetherWith ExitTransition.None
            else (fadeIn() + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.6f))
        },
        modifier = modifier.size(size).semantics { contentDescription = description },
        label = "stato",
    ) { sp -> Canvas(Modifier.size(size)) { drawBadge(sp, this.size.minDimension, alpha = alpha, outline = ambient) } }
}

fun DrawScope.drawBadge(spec: Badge.Spec, d: Float, alpha: Float = 1f, outline: Boolean = false) {
    // In ambient: solo il contorno, nessun riempimento pieno (design, sezione 2).
    val fill = Color(spec.fill).copy(alpha = alpha)
    val ink = if (outline) Color(spec.fill).copy(alpha = alpha) else Color(spec.glyphColor).copy(alpha = alpha)
    val stroke = Stroke(width = d * 0.08f)
    when (spec.shape) {
        Badge.Shape.CIRCLE -> if (outline) drawCircle(fill, radius = d / 2f - d * 0.04f, center = Offset(d / 2f, d / 2f), style = stroke)
            else drawCircle(fill, radius = d / 2f, center = Offset(d / 2f, d / 2f))
        Badge.Shape.SQUARE -> if (outline) drawRoundRect(fill, size = Size(d * 0.92f, d * 0.92f), topLeft = Offset(d * 0.04f, d * 0.04f), cornerRadius = CornerRadius(d * 0.23f), style = stroke)
            else drawRoundRect(fill, size = Size(d, d), cornerRadius = CornerRadius(d * 0.23f))
    }
    // Glifo a 0,44 del diametro invece di 0,52 più cappuccio: prima toccava il bordo del cerchio e del quadrato
    // (Franz, 13/09 21:11). Le stesse proporzioni in `BadgeBitmap`, così lista e notifiche restano identiche.
    val c = Offset(d / 2f, d / 2f); val r = d * 0.22f; val w = d * 0.095f
    glyph(spec.glyph, c, r, w, ink)
}

/** I quattro glifi di stato, con lo stesso disegno dovunque vadano: badge, riga della lista, notifica. */
private fun DrawScope.glyph(g: Badge.Glyph, c: Offset, r: Float, w: Float, ink: Color) {
    when (g) {
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
