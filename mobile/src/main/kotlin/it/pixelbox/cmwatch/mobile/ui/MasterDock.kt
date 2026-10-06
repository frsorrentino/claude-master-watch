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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawBehind
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
 * La master è un foglio sollevato sulla rampa grigio-blu delle card, un passo più chiaro (Profondità, Franz 06/10 20:09-20:12):
 * la luce batte sul bordo in alto della linguetta, una piega la separa dal fondo del campo, il foglio getta l'ombra sulla
 * lista e, aperta, la conversazione è una vasca che scende a un blu-nero più profondo. Gli stessi valori della web app.
 */
internal val MasterLip = androidx.compose.ui.graphics.Color(0xFF3B4048)
internal val MasterTab = androidx.compose.ui.graphics.Color(0xFF30343C)
internal val MasterDisc = androidx.compose.ui.graphics.Color(0xFF444953)
internal val MasterSheet = androidx.compose.ui.graphics.Color(0xFF252930)
internal val MasterSheetLow = androidx.compose.ui.graphics.Color(0xFF1F232A)
internal val MasterWell = androidx.compose.ui.graphics.Color(0xFF1B1F26)
internal val MasterDusk = androidx.compose.ui.graphics.Color(0xFF161A20)
internal val MasterDeep = androidx.compose.ui.graphics.Color(0xFF0C0E13)
internal val MasterRim = androidx.compose.ui.graphics.Color(0x29DEE9FF)
internal val MasterGlint = androidx.compose.ui.graphics.Color(0xE6EAF2FF)
internal val MasterCrease = androidx.compose.ui.graphics.Color(0x8C000000)
internal val MasterLine = androidx.compose.ui.graphics.Color(0x33DEE9FF)

/** La scintilla di Claude, l'unico colore della master: otto raggi, gira mentre la master lavora o aspetta un permesso. */
@Composable
internal fun MasterSpark(state: it.pixelbox.cmwatch.contract.SessionState, size: androidx.compose.ui.unit.Dp = 11.dp) {
    val spin = it.pixelbox.cmwatch.rules.Badge.breathes(state) && !animationsOff()
    val angle = if (spin) {
        val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "spark")
        t.animateFloat(0f, 360f, androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(2400, easing = androidx.compose.animation.core.LinearEasing)), label = "spark").value
    } else 0f
    androidx.compose.foundation.Canvas(Modifier.size(size).graphicsLayer { rotationZ = angle }) {
        val c = center; val r = this.size.minDimension / 2f; val w = r * 2.1f / 9f
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val inner = r * 0.24f; val dx = kotlin.math.cos(a).toFloat(); val dy = kotlin.math.sin(a).toFloat()
            drawLine(CmColors.modelOpus, androidx.compose.ui.geometry.Offset(c.x + dx * inner, c.y + dy * inner),
                androidx.compose.ui.geometry.Offset(c.x + dx * (r - w / 2), c.y + dy * (r - w / 2)), strokeWidth = w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

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
        if (!expanded && look.thread) MasterThread()
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
            Modifier.fillMaxWidth()
                // Chiusa, il foglio sta sopra la lista e le getta l'ombra (fuori dal ritaglio, sopra la linguetta).
                .then(if (expanded) Modifier else Modifier.drawBehind {
                    val h = 30.dp.toPx()
                    drawRect(
                        androidx.compose.ui.graphics.Brush.verticalGradient(0f to androidx.compose.ui.graphics.Color.Transparent, 1f to androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f), startY = -h, endY = 0f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, -h), size = androidx.compose.ui.geometry.Size(size.width, h),
                    )
                })
                .clip(shape)
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(0f to MasterLip, 0.62f to MasterTab))
                .drawWithContent {
                    drawContent()
                    // Il filo di luce sul bordo in alto, più vivo al centro, e la piega in basso.
                    val px = 1.dp.toPx()
                    drawRect(MasterRim, size = androidx.compose.ui.geometry.Size(size.width, px))
                    drawRect(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(0f to androidx.compose.ui.graphics.Color.Transparent, 0.5f to MasterGlint, 1f to androidx.compose.ui.graphics.Color.Transparent, startX = size.width * 0.18f, endX = size.width * 0.82f),
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.18f, 0f), size = androidx.compose.ui.geometry.Size(size.width * 0.64f, px),
                    )
                    drawRect(MasterCrease, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - px), size = androidx.compose.ui.geometry.Size(size.width, px))
                }
                .then(drag)
                .handCursor().clickable(onClick = onToggle).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // La firma: l'icona della master con l'anello corallo-lilla (Franz, 05/10 11:30).
            if (look.signature) Box(Modifier.size(28.dp).border(2.dp, MasterGradient, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { SessionBadge(master, 18.dp) }
            else SessionBadge(master, 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MasterSpark(master.state)
                    Text(label, style = MonoSmall.copy(color = CmColors.text2), maxLines = 1, overflow = TextOverflow.Clip)
                }
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
            if (spoken != null) FilledTonalIconButton(onClick = { onSpeak(spoken) }, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MasterDisc)) {
                Icon(
                    if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    stringResource(if (reading) R.string.stop_reading else R.string.dock_listen), tint = CmColors.text,
                )
            }
            FilledTonalIconButton(onClick = onToggle, modifier = Modifier.size(44.dp), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MasterDisc)) {
                Icon(
                    if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                    stringResource(if (expanded) R.string.dock_collapse else R.string.dock_conversation), tint = CmColors.text,
                )
            }
        }
        if (expanded && look.thread) MasterThread()
    }
}
