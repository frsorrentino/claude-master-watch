package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.wear.ui.theme.BriefNumber
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/**
 * Card nello stile del «brief mattutino» di Wear OS, copiata dai fotogrammi della sua schermata (13/09 16:25):
 * etichetta verde pallida, numero grande con l'unità piccola accanto, riga secondaria grigia, pillolina piena e
 * anello a destra. L'anello si disegna solo quando il dato ha una percentuale: un anello finto non dice nulla.
 */
@Composable
fun BriefCard(
    card: BriefCards.Card,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    /** Vero quando la card è nell'inquadratura: da lì parte il riempimento dell'arco (Franz, 16/09 01:45). */
    visible: Boolean = true,
    onClick: () -> Unit = {},
) {
    val ink = if (card.tone == BriefCards.Tone.STALE) CmColors.stale else CmColors.briefBig
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CmColors.briefCard, contentColor = ink),
        contentPadding = PaddingValues(start = 15.dp, top = 13.dp, end = 8.dp, bottom = 13.dp),
        transformation = transformation,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val tinta = if (card.tone == BriefCards.Tone.STALE) CmColors.stale else CmColors.briefLabel
                    Text(card.label, style = MaterialTheme.typography.labelMedium, color = tinta, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    // Il segno dell'account dopo il titolo, «Quota ●» (Franz, 16/09 15:26), come in «Ritmo 5 ore ●».
                    card.shape?.let { Spacer(Modifier.width(6.dp)); AccountMark(it, tinta) }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    // Il numero grande rotola quando cambia (proposta 19, fase 1): 7 % → 8 % si nota anche di sfuggita.
                    RollingText(card.value, style = BriefNumber, color = ink, animate = animate)
                    card.unit?.let {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            it, style = MaterialTheme.typography.bodyMedium, color = CmColors.briefSecondary,
                            maxLines = 1, modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
                card.secondary?.let {
                    Text(
                        it, style = MaterialTheme.typography.bodySmall, color = CmColors.briefSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (card.progress2 == null) card.pill?.let {
                    Spacer(Modifier.height(8.dp))
                    Pill(it, card.tone)
                }
            }
            card.progress?.let { p ->
                Spacer(Modifier.width(8.dp))
                Gauge(
                    progress = p, tone = card.tone, glyph = card.glyph, animate = animate, visible = visible,
                    second = card.progress2, secondTone = card.tone2,
                )
            }
        }
        // Con il doppio anello la pillola è la legenda dell'anello interno: stesso colore, e sotto la riga dell'anello
        // a tutta larghezza, su una riga sola. Nella colonna accanto all'anello «settimana 82 %» andava a capo (11:52).
        if (card.progress2 != null) card.pill?.let {
            Spacer(Modifier.height(8.dp))
            WeekPill(it, card.tone2)
        }
        // La ripartenza settimanale centrata sotto tutta la card (S07): a sinistra, in fondo a una card alta, il bordo
        // tondo la tagliava («reset Thu 02:00» nello snapshot della Quota); al centro resta dentro la corda del cerchio.
        card.note?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it, style = MaterialTheme.typography.bodySmall, color = CmColors.briefSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Cerchio per l'account personale, quadrato per quello di lavoro, come nella tile (Franz, 16/09 14:41). Vuoti, 10 dp e
 * quadrato ad angoli vivi (16/09 15:47): pieni da 8 dp, con gli angoli smussati, il quadrato si leggeva quasi come un cerchio.
 */
@Composable
fun AccountMark(shape: BriefCards.Shape, color: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier.size(10.dp).border(
            1.5.dp, color, if (shape == BriefCards.Shape.CIRCLE) RoundedCornerShape(percent = 50) else androidx.compose.ui.graphics.RectangleShape,
        )
    )
}

/** La pillola della settimana, del colore dell'anello interno: lavanda, oppure ambra e rosso sopra le soglie. */
@Composable
private fun WeekPill(text: String, tone: BriefCards.Tone) {
    val (bg, ink) = when (tone) {
        BriefCards.Tone.WARN -> CmColors.briefWarn to CmColors.briefWarnInk
        BriefCards.Tone.ALERT -> CmColors.briefAlert to CmColors.briefAlertInk
        BriefCards.Tone.STALE -> CmColors.line to CmColors.text2
        else -> CmColors.briefWeek to CmColors.briefWeekInk
    }
    Box(Modifier.background(bg, RoundedCornerShape(percent = 50)).padding(horizontal = 12.dp, vertical = 3.dp)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Pillolina piena come nel brief: fondo chiaro e inchiostro scuro, così si legge anche di giorno. */
@Composable
private fun Pill(text: String, tone: BriefCards.Tone) {
    val (bg, ink) = when (tone) {
        BriefCards.Tone.GOOD -> CmColors.briefGood to CmColors.briefGoodInk
        BriefCards.Tone.WARN -> CmColors.briefWarn to CmColors.briefWarnInk
        BriefCards.Tone.ALERT -> CmColors.briefAlert to CmColors.briefAlertInk
        BriefCards.Tone.STALE -> CmColors.line to CmColors.text2
        BriefCards.Tone.NEUTRAL -> CmColors.briefChip to CmColors.briefChipInk
    }
    Box(
        Modifier.background(bg, RoundedCornerShape(percent = 50)).padding(horizontal = 12.dp, vertical = 3.dp)
    ) {
        // Due righe come le pilloline del brief («1 sopra l'obiettivo»): la card cresce, il testo non si taglia.
        Text(text, style = MaterialTheme.typography.labelSmall, color = ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
