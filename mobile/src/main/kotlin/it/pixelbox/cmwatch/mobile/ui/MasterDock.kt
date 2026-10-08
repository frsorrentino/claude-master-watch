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
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloat
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
 * La master è la superficie più alta dell'app, velata del celeste d'accento come vuole l'elevazione tonale del Material 3:
 * i toni dei ruoli di superficie (T22 più alta, T17 alta, T12 contenitore, T10 bassa, T30 tasti) generati dal celeste
 * d'accento #A8C7FA, tinta 273, croma 14; piatti, nessun effetto di luce (Celeste velato, Franz 06/10 21:15-22:27).
 * Gli stessi valori della web app.
 */
internal val MasterHighest = androidx.compose.ui.graphics.Color(0xFF283549)
internal val MasterHigh = androidx.compose.ui.graphics.Color(0xFF1D2B3E)
internal val MasterContainer = androidx.compose.ui.graphics.Color(0xFF122033)
internal val MasterLow = androidx.compose.ui.graphics.Color(0xFF0E1C2E)
internal val MasterKey = androidx.compose.ui.graphics.Color(0xFF3B475C)

/**
 * I tasti tondi della master cambiano forma quando li premi, come quelli del Material 3 Expressive: da cerchio a quadrato
 * smussato, con la molla veloce del tema (Franz, 06/10 22:27).
 */
@Composable
internal fun rememberMorphShape(source: androidx.compose.foundation.interaction.MutableInteractionSource): androidx.compose.ui.graphics.Shape {
    val pressed by source.collectIsPressedAsState()
    val radius by androidx.compose.animation.core.animateDpAsState(if (pressed) 12.dp else 22.dp, MaterialTheme.motionScheme.fastSpatialSpec(), label = "morph")
    return RoundedCornerShape(radius)
}

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
fun MasterDock(
    master: Session, hero: MasterHome.Hero?, onSpeak: (String) -> Unit, onToggle: () -> Unit, expanded: Boolean = false,
    /**
     * Le tre misure del tablet (Franz, 08/10 18:15): il trascinamento in su e in giù va alla misura vicina, non solo aperta e
     * chiusa. Null = il tocco basta, come sul telefono.
     */
    onUp: (() -> Unit)? = null, onDown: (() -> Unit)? = null,
    /** Il tasto tondo a destra: ▲ apre di più; null = segue `expanded`. A metà dice ▲ e porta a tutto schermo. */
    toggleUp: Boolean = !expanded, onToggleKey: () -> Unit = onToggle,
    /**
     * La master a tutto schermo sul telefono (Franz, 08/10 18:15): una testata sola, nel colore della master fino alla barra di
     * stato. ⌄ va a sinistra, al posto della freccia indietro, e a destra il menu ≡ della home (`trailing`); angoli dritti.
     */
    screen: Boolean = false, trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    val time = hero?.at?.let { DOCK_HM.format(Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault())) }
    val ctx = master.context?.let { stringResource(R.string.ctx_short, it) }
    // Aperta, modello e contesto stanno già nella riga sotto la linguetta: qui resta l'ora. Chiusa, l'ora e il contesto, che
    // conta per l'handoff: il modello cambia di rado, e con lui la riga non stava nella larghezza (Franz, 06/10 22:27).
    val label = (if (expanded) listOfNotNull(stringResource(R.string.dock_master), time) else listOfNotNull(stringResource(R.string.dock_master), time, ctx)).joinToString(" · ")
    // Aperta resta una linguetta col verso di quando è chiusa, angoli tondi in alto (Franz, 06/10 08:48).
    val shape = if (screen) androidx.compose.ui.graphics.RectangleShape else RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    Column(Modifier.fillMaxWidth()) {
        val look = LocalMasterLook.current
        if (!expanded && look.thread) MasterThread()
        // Franz, 03/10 17:05: anche col gesto. Trascinare in su la barra ridotta la espande, trascinarla in giù da espansa la
        // riduce. Sulla barra e non dal bordo dello schermo, dove Android tiene il gesto per la schermata Home.
        val threshold = with(androidx.compose.ui.platform.LocalDensity.current) { 40.dp.toPx() }
        val up by androidx.compose.runtime.rememberUpdatedState(onUp)
        val down by androidx.compose.runtime.rememberUpdatedState(onDown)
        val drag = Modifier.pointerInput(expanded) {
            var total = 0f
            detectVerticalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = {
                    when {
                        total < -threshold -> up?.invoke() ?: run { if (!expanded) onToggle() }
                        total > threshold -> down?.invoke() ?: run { if (expanded) onToggle() }
                    }
                },
                onVerticalDrag = { change, dy -> total += dy; change.consume() },
            )
        }
        Row(
            Modifier.fillMaxWidth().clip(shape).background(MasterHighest)
                .then(drag)
                .handCursor().clickable(onClick = onToggle).padding(start = 8.dp, end = if (screen) 4.dp else 12.dp, top = 10.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val toggleKey: @Composable () -> Unit = {
                val toggleSrc = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                FilledTonalIconButton(onClick = onToggleKey, modifier = Modifier.size(44.dp), shape = rememberMorphShape(toggleSrc), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MasterKey), interactionSource = toggleSrc) {
                    Icon(
                        if (toggleUp) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        stringResource(if (toggleUp) R.string.dock_conversation else R.string.dock_collapse), tint = CmColors.text,
                    )
                }
            }
            // La freccia sempre a sinistra, aperta o chiusa (Franz, 08/10 20:42: cambiava lato e non la ritrovava).
            toggleKey()
            // La firma: l'icona della master con l'anello corallo-lilla (Franz, 05/10 11:30).
            // Il badge di stato solo quando la master ti aspetta o è chiusa; altrimenti la scintilla di Claude, che gira mentre
            // lavora. Il cerchio rosso era l'elemento più saturo della schermata e nel Material 3 il rosso è «errore» (22:27).
            if (master.state == it.pixelbox.cmwatch.contract.SessionState.WAITING || master.state == it.pixelbox.cmwatch.contract.SessionState.GONE) SessionBadge(master, 20.dp)
            else MasterSpark(master.state, 20.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MonoSmall.copy(color = CmColors.text2), maxLines = 1, overflow = TextOverflow.Clip)
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
            val speakSrc = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            if (spoken != null) FilledTonalIconButton(onClick = { onSpeak(spoken) }, modifier = Modifier.size(44.dp), shape = rememberMorphShape(speakSrc), colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MasterKey), interactionSource = speakSrc) {
                Icon(
                    if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    stringResource(if (reading) R.string.stop_reading else R.string.dock_listen), tint = CmColors.text,
                )
            }
            trailing?.invoke(this)
        }
        if (expanded && look.thread) MasterThread()
    }
}
