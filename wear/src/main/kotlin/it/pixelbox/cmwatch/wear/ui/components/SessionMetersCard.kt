package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.SessionMeters
import it.pixelbox.cmwatch.wear.ui.theme.BriefNumber
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import kotlinx.coroutines.delay

/**
 * Le misure della sessione in una card sola (Franz, 16/09 08:29: le tre righe etichetta-valore non erano integrate,
 * «ogni misura dovrebbe avere la sua grafica»). Il contesto è il protagonista — numero grande e barra che si riempie
 * entrando nell'inquadratura, con le due soglie segnate sul binario; sotto, il modello come pillola col pallino di
 * famiglia e l'effort come tre tacche che si accendono una dopo l'altra. Quello che il PC non manda non si disegna.
 */
@Composable
fun SessionMetersCard(
    session: Session,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visible: Boolean = true,
) {
    val nome = ModelText.short(session.model)
    val passo = SessionMeters.effortStep(session.effort)
    val frazione = SessionMeters.contextFraction(session.context)
    if (frazione == null && nome == null && passo == null) return
    val tono = SessionMeters.contextTone(session.context)
    Card(
        onClick = {},
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CmColors.briefCard, contentColor = CmColors.briefBig),
        contentPadding = PaddingValues(start = 15.dp, top = 13.dp, end = 15.dp, bottom = 13.dp),
        transformation = transformation,
    ) {
        frazione?.let { f ->
            Text(
                stringResource(R.string.card_context), style = MaterialTheme.typography.labelMedium,
                color = CmColors.briefLabel, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                // Il numero rotola come quelli del brief: 43 % → 44 % si nota anche di sfuggita.
                RollingText("${session.context}", style = BriefNumber, color = inchiostro(tono), animate = animate)
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.card_context_unit), style = MaterialTheme.typography.bodyMedium,
                    color = CmColors.briefSecondary, maxLines = 1, modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            ContextBar(f, tono, animate = animate, visible = visible)
        }
        if (nome != null || passo != null) {
            Spacer(Modifier.height(if (frazione == null) 0.dp else 12.dp))
            Row(
                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                nome?.let { ModelPill(it, ModelText.family(session.model), ModelText.millionWindow(session.model)) }
                passo?.let { EffortSteps(it, session.effort ?: "", animate = animate, visible = visible) }
            }
        }
    }
}

/**
 * La barra del contesto: binario scuro, riempimento che parte da sinistra quando la card entra nell'inquadratura, e
 * due tacche scure sul binario dove stanno le soglie (75 % e 90 %). Le tacche dicono quanto manca senza scrivere numeri.
 */
@Composable
private fun ContextBar(fraction: Float, tone: BriefCards.Tone, animate: Boolean, visible: Boolean) {
    val target = fraction.coerceIn(0f, 1f)
    val mostrato = remember { Animatable(if (animate) 0f else target) }
    val molla = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(target, animate, visible) {
        when {
            !animate -> mostrato.snapTo(target)
            visible -> mostrato.animateTo(target, molla)
            else -> mostrato.snapTo(0f)
        }
    }
    val ink = inchiostro(tone)
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val r = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(CmColors.briefTrack, size = size, cornerRadius = r)
        val larghezza = size.width * mostrato.value
        // Sotto l'altezza della barra il riempimento diventa un ovale schiacciato: si tiene almeno tondo.
        if (larghezza > 0f) {
            drawRoundRect(ink, size = Size(maxOf(larghezza, size.height), size.height), cornerRadius = r)
        }
        listOf(0.75f, 0.90f).forEach { soglia ->
            drawRoundRect(
                CmColors.briefCard,
                topLeft = Offset(size.width * soglia - 1.dp.toPx(), 0f),
                size = Size(2.dp.toPx(), size.height),
            )
        }
    }
}

/** Le tre tacche dell'effort, crescenti come il segnale: accese fino al livello, e si accendono una dopo l'altra. */
@Composable
private fun EffortSteps(step: Int, label: String, animate: Boolean, visible: Boolean) {
    // La molla si legge qui, non dentro `LaunchedEffect`: là non c'è più contesto composabile e non compila.
    val molla = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        (1..SessionMeters.EFFORT_STEPS).forEach { i ->
            val accesa = i <= step
            val luce = remember(i) { Animatable(if (animate) 0f else 1f) }
            LaunchedEffect(accesa, animate, visible) {
                when {
                    !animate -> luce.snapTo(1f)
                    visible -> { delay(90L * (i - 1)); luce.animateTo(1f, molla) }
                    else -> luce.snapTo(0f)
                }
            }
            Box(
                Modifier
                    .width(5.dp)
                    .height(6.dp + 5.dp * (i - 1))
                    .graphicsLayer {
                        // La tacca cresce dal basso mentre si accende: l'occhio legge la scala prima del numero.
                        scaleY = if (accesa) luce.value.coerceAtLeast(0.15f) else 1f
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                        alpha = if (accesa) luce.value else 1f
                    }
                    .background(
                        if (accesa) CmColors.primary else CmColors.briefTrack,
                        RoundedCornerShape(2.dp),
                    )
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = CmColors.briefSecondary, maxLines = 1)
    }
}

/** La pillola del modello: pallino di famiglia, nome breve e, se la finestra è quella grande, la targhetta «1M». */
@Composable
private fun ModelPill(name: String, family: ModelText.Family?, million: Boolean) {
    Row(
        Modifier.clip(RoundedCornerShape(percent = 50)).background(CmColors.line).padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(colore(family), RoundedCornerShape(percent = 50)))
        Spacer(Modifier.width(6.dp))
        Text(name, style = MaterialTheme.typography.labelMedium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (million) {
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.card_window_1m), style = MaterialTheme.typography.labelSmall,
                color = CmColors.briefChipInk, maxLines = 1,
                modifier = Modifier.clip(RoundedCornerShape(percent = 50)).background(CmColors.briefChip)
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

private fun colore(family: ModelText.Family?): Color = when (family) {
    ModelText.Family.OPUS -> CmColors.modelOpus
    ModelText.Family.SONNET -> CmColors.modelSonnet
    ModelText.Family.HAIKU -> CmColors.modelHaiku
    else -> CmColors.modelOther
}

private fun inchiostro(tone: BriefCards.Tone): Color = when (tone) {
    BriefCards.Tone.ALERT -> CmColors.briefAlertRing
    BriefCards.Tone.WARN -> CmColors.briefWarn
    else -> CmColors.briefRing
}
