package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DOCK_HM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * La master nella home (design 03/10, tavola 4; Franz 16:30-16:44): ridotta è la barra agganciata sopra «Scrivi alla
 * master» (badge, «MASTER · ora · modello · contesto», il titolo dell'ultimo esito, ▶ e ▲); il tocco la espande a tutta
 * pagina, dove la stessa barra sta in cima con ▼, e un altro tocco la riduce; lo stesso col trascinamento in su e in giù.
 */
@Composable
fun MasterDock(master: Session, hero: MasterHome.Hero?, onSpeak: (String) -> Unit, onToggle: () -> Unit, expanded: Boolean = false) {
    val time = hero?.at?.let { DOCK_HM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) }
    val label = listOfNotNull(stringResource(R.string.dock_master), time, ModelText.short(master.model), master.context?.let { "$it%" }).joinToString(" · ")
    val shape = if (expanded) RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp) else RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Column(Modifier.fillMaxWidth()) {
        if (!expanded) Box(Modifier.fillMaxWidth().height(1.dp).background(CmColors.line))
        // Franz, 03/10 17:05: anche col gesto. Trascinare in su la barra ridotta la espande, trascinarla in giù da espansa la
        // riduce. Sulla barra e non dal bordo dello schermo, dove Android tiene il gesto per la schermata Home.
        val threshold = with(androidx.compose.ui.platform.LocalDensity.current) { 40.dp.toPx() }
        val drag = Modifier.pointerInput(expanded) {
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = { if ((!expanded && total < -threshold) || (expanded && total > threshold)) onToggle() },
                onVerticalDrag = { change, dy -> total += dy; change.consume() },
            )
        }
        Row(
            Modifier.fillMaxWidth().clip(shape).background(CmColors.surfaceLow).then(drag)
                .clickable(onClick = onToggle).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SessionBadge(master, 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MonoSmall, maxLines = 1, overflow = TextOverflow.Clip)
                hero?.let {
                    Text(
                        linked(it.headline), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip,
                    )
                }
            }
            // Durante la lettura il ▶ diventa ■ e la ferma (Franz, 03/10 17:25), come sotto le risposte della chat.
            val spoken = hero?.let { h -> listOf(h.headline, h.body).filter { it.isNotBlank() }.joinToString("\n") }
            val reading = spoken != null && LocalSpeaking.current == spoken
            if (spoken != null) FilledTonalIconButton(onClick = { onSpeak(spoken) }, modifier = Modifier.size(44.dp)) {
                Icon(
                    if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    stringResource(if (reading) R.string.stop_reading else R.string.dock_listen), tint = CmColors.actionIcon,
                )
            }
            FilledTonalIconButton(onClick = onToggle, modifier = Modifier.size(44.dp)) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    stringResource(if (expanded) R.string.dock_collapse else R.string.dock_conversation), tint = CmColors.actionIcon,
                )
            }
        }
        if (expanded) Box(Modifier.fillMaxWidth().height(1.dp).background(CmColors.line))
    }
}
