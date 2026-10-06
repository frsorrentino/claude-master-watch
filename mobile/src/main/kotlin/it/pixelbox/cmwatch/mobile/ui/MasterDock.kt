package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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

/** La master è un blocco terracotta a sé: la linguetta appena più chiara del fondo intorno al campo (variante B, Franz, 06/10 18:19). */
internal val MasterTab = androidx.compose.ui.graphics.Color(0xFF36221A)
internal val MasterBed = androidx.compose.ui.graphics.Color(0xFF24160F)
internal val MasterField = androidx.compose.ui.graphics.Color(0xFF180E0A)
private val MasterSoft = androidx.compose.ui.graphics.Color(0xFFE2A58C)
private val MasterIcon = androidx.compose.ui.graphics.Color(0xFFF2C1A8)

/**
 * La master nella home (design 03/10, tavola 4; Franz 16:30-16:44): ridotta è la barra agganciata sopra «Scrivi alla
 * master» (badge, «MASTER · ora · modello · contesto», il titolo dell'ultimo esito, ▶ e ▲); il tocco la espande a tutta
 * pagina, dove la stessa barra sta in cima con ▼, e un altro tocco la riduce; lo stesso col trascinamento in su e in giù.
 */
@Composable
fun MasterDock(master: Session, hero: MasterHome.Hero?, onSpeak: (String) -> Unit, onToggle: () -> Unit, expanded: Boolean = false) {
    val time = hero?.at?.let { DOCK_HM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) }
    val ctx = master.context?.let { stringResource(R.string.ctx_short, it) }
    val label = listOfNotNull(stringResource(R.string.dock_master), time, ModelText.short(master.model), ctx).joinToString(" · ")
    // Aperta resta una linguetta col verso di quando è chiusa, angoli tondi in alto (Franz, 06/10 08:48).
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Column(Modifier.fillMaxWidth()) {
        val look = LocalMasterLook.current
        if (!expanded) { if (look.thread) MasterThread() else Box(Modifier.fillMaxWidth().height(1.dp).background(CmColors.line)) }
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
            Modifier.fillMaxWidth().clip(shape).background(MasterTab).then(drag)
                .handCursor().clickable(onClick = onToggle).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // La firma: l'icona della master con l'anello corallo-lilla (Franz, 05/10 11:30).
            if (look.signature) Box(Modifier.size(28.dp).border(2.dp, MasterGradient, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { SessionBadge(master, 18.dp) }
            else SessionBadge(master, 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MonoSmall.copy(color = MasterSoft), maxLines = 1, overflow = TextOverflow.Clip)
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
            if (reading) RatePill()
            if (spoken != null) FilledTonalIconButton(onClick = { onSpeak(spoken) }, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = CmColors.modelOpus.copy(alpha = 0.18f))) {
                Icon(
                    if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    stringResource(if (reading) R.string.stop_reading else R.string.dock_listen), tint = MasterIcon,
                )
            }
            FilledTonalIconButton(onClick = onToggle, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = CmColors.modelOpus.copy(alpha = 0.18f))) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    stringResource(if (expanded) R.string.dock_collapse else R.string.dock_conversation), tint = MasterIcon,
                )
            }
        }
        if (expanded) { if (look.thread) MasterThread() else Box(Modifier.fillMaxWidth().height(1.dp).background(CmColors.line)) }
    }
}
