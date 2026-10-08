package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ReadingBar
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il mini-controller in sovraimpressione (Franz, 04/10 00:18): uno solo sopra tutte le schermate, che resta fermo mentre
 * le pagine cambiano sotto. `bar` lo mette `MainActivity` (null quando la voce tace); i posti sopra il campo (`ReadingSlot`)
 * dicono dove posarlo, senza posto sta in fondo.
 */
class ReadingOverlay {
    var bar by mutableStateOf<(@Composable () -> Unit)?>(null)
    /**
     * Lo slider della velocità aperto nella barra (Franz, 07/10, approvata alle 22:06). Sta qui perché lo apre anche la
     * pillola «1,5×» accanto al ▶ di un messaggio: c'è una barra sola, e un comando solo per la velocità.
     */
    var rateOpen by mutableStateOf(false)
    /** L'ultimo tocco sullo slider: senza tocchi per 3 s si richiude in pillola. */
    var rateTouched by mutableLongStateOf(0L)
    /** La sessione da cui legge: sul tablet, con più campi in vista, il controller va sopra il suo. */
    var source by mutableStateOf<String?>(null)
    internal var heightPx by mutableIntStateOf(0)
    internal val slots = mutableStateMapOf<Any, Pair<String?, Rect>>()
}

val LocalReadingOverlay = staticCompositionLocalOf<ReadingOverlay?> { null }

/** Lo spazio sopra il campo della sessione `name` (`SessionSheet`) dove il mini-controller si posa; lo disegna l'host. */
@Composable
fun ReadingSlot(name: String?, modifier: Modifier = Modifier) {
    val o = LocalReadingOverlay.current ?: return
    if (o.bar == null) return
    val key = remember { Any() }
    DisposableEffect(key) { onDispose { o.slots.remove(key) } }
    val h = with(LocalDensity.current) { if (o.heightPx > 0) o.heightPx.toDp() else 60.dp }
    // I bordi veri del posto, non quelli tagliati dallo schermo (Franz, 04/10 22:15): durante lo swipe la pagina che esce,
    // tagliata, sembrava una colonna stretta, e il controller si restringeva con lei per poi riapparire nella pagina dopo.
    Spacer(modifier.fillMaxWidth().height(h).onGloballyPositioned { c ->
        val r = name to Rect(c.positionInWindow(), c.size.toSize())
        if (o.slots[key] != r) o.slots[key] = r
    })
}

/**
 * Il controller sopra tutto. Fra più posti vale uno in vista per almeno metà della sua larghezza: quello della sessione che
 * legge, se c'è (le colonne del tablet), se no il più in vista e poi il più a sinistra (le pagine vicine dello scorrimento).
 * Il salto fra un posto e l'altro, o verso il fondo, scivola invece di riapparire. Con la tastiera aperta si fa da parte.
 */
@Composable
fun ReadingOverlayHost(o: ReadingOverlay, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val imeOpen = WindowInsets.ime.getBottom(density) > 0
    val bottomPx = WindowInsets.navigationBars.getBottom(density) + with(density) { 12.dp.roundToPx() }
    var root by remember { mutableStateOf(Rect.Zero) }
    var last by remember { mutableStateOf(o.bar) }
    o.bar?.let { last = it }
    Box(modifier.fillMaxSize().onGloballyPositioned { root = it.boundsInWindow() }) {
        val seen = { r: Rect -> if (r.width <= 0f) 0f else ((minOf(r.right, root.right) - maxOf(r.left, root.left)) / r.width).coerceAtLeast(0f) }
        val visible = o.slots.values.filter { (_, r) -> seen(r) >= 0.5f }
        val slot = (visible.firstOrNull { (n, _) -> n != null && n == o.source } ?: visible.sortedWith(compareByDescending<Pair<String?, Rect>> { seen(it.second) }.thenBy { it.second.left }).firstOrNull())?.second
        val lift = slot?.let { (root.bottom - it.bottom).roundToInt() } ?: bottomPx
        // Fermo durante lo swipe (Franz, 04/10 22:15): finché la pagina scelta scorre, il controller resta all'altezza
        // dell'ultima pagina ferma; a pagina posata scivola alla sua altezza, se è diversa.
        val moving = slot != null && root.width > 0f && slot.width >= root.width * 0.9f && abs(slot.left - root.left) > 1f
        var steady by remember { mutableIntStateOf(lift) }
        LaunchedEffect(moving, lift) { if (!moving) steady = lift }
        val shown by animateIntAsState(if (moving) steady else lift, tween(220, easing = FastOutSlowInEasing), label = "readingLift")
        AnimatedVisibility(
            o.bar != null && !imeOpen, Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            // Largo quanto il suo posto quando il posto è una colonna (la conversazione del tablet, le colonne). Un posto largo
            // quanto lo schermo è una pagina del telefono: lì il controller resta fermo a tutta larghezza e non segue la pagina
            // che scorre (Franz, 04/10 15:09: seguendo la x del posto «si ricompone a ogni swipe»). Senza posto, in fondo al
            // centro e mai più largo di 640 dp.
            Box(Modifier.fillMaxWidth()) {
                val column = slot?.takeIf { s -> root.width > 0f && s.width < root.width * 0.9f }
                val place = column?.let { s -> Modifier.align(Alignment.BottomStart).offset { IntOffset((s.left - root.left).roundToInt(), -shown) }.width(with(density) { s.width.toDp() }) }
                    ?: Modifier.align(Alignment.BottomCenter).offset { IntOffset(0, -shown) }.widthIn(max = if (slot != null) Dp.Infinity else 640.dp)
                Box(place.padding(horizontal = 12.dp).onSizeChanged { o.heightPx = it.height }) { last?.invoke() }
            }
        }
    }
}

/**
 * Il mini-controller della lettura (Franz, 03/10 23:00): su ogni schermata finché la voce legge. Le barrette che si
 * muovono, da dove arriva il testo e la sua prima riga (il tocco riporta lì), la velocità, la voce e ■ per fermare.
 * Velocità e voce arrivano come parametri: il controller si disegna nell'overlay, fuori dai `CompositionLocal` della
 * lettura (Franz, 04/10 20:28: lì il tasto della velocità leggeva sempre 1× e il tocco non faceva niente).
 */
@Composable
fun ReadingPill(
    source: String?, text: String, onOpen: (() -> Unit)?, onStop: () -> Unit,
    rate: Float, onRate: (Float) -> Unit,
    /** La voce di adesso (null = la predefinita) e il tocco che passa alla dopo; null senza voci italiane da scegliere. */
    voice: String?, onVoice: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** In pausa: ⏸ diventa ▶ pieno, le barrette si fermano, sopra il testo «in pausa» (Franz, 07/10 22:06). */
    paused: Boolean = false, onPause: () -> Unit = {}, onResume: () -> Unit = {},
) {
    val overlay = LocalReadingOverlay.current
    var localOpen by remember { mutableStateOf(false) }
    val open = overlay?.rateOpen ?: localOpen
    fun setOpen(v: Boolean) { if (overlay != null) { overlay.rateOpen = v; overlay.rateTouched = System.currentTimeMillis() } else localOpen = v }
    var touched by remember { mutableLongStateOf(0L) }
    val lastTouch = overlay?.rateTouched ?: touched
    // Senza tocchi per 3 s lo slider torna pillola.
    LaunchedEffect(open, lastTouch) { if (open) { kotlinx.coroutines.delay(3_000); setOpen(false) } }
    var draft by remember(open) { mutableFloatStateOf(rate) }
    // Mentre si trascina la voce cambia velocità quando il dito si ferma un attimo (Franz, 08/10 19:50: «sentirla variare
    // in tempo reale»): il motore non cambia velocità a metà frase, quindi riparte dal pezzo che sta dicendo.
    LaunchedEffect(draft, open) {
        if (!open) return@LaunchedEffect
        kotlinx.coroutines.delay(LIVE_RATE_MS)
        val v = it.pixelbox.cmwatch.rules.SpeechRate.snap(draft)
        if (v != rate) onRate(v)
    }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = CmColors.surfaceHigh, shadowElevation = 6.dp) {
        BoxWithConstraints {
            // In una colonna stretta del tablet restano i tasti: la riga del testo si vede già nella colonna.
            val roomy = maxWidth >= 300.dp
            Row(
                Modifier.heightIn(min = 60.dp).padding(start = if (open) 8.dp else 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (open) {
                    // La velocità aperta: il valore, lo slider da 0,5× a 2× con il segno di 1×, poi pausa e stop.
                    RateValue(draft, onClick = { setOpen(false) })
                    RateSlider(
                        draft,
                        onChange = { v -> draft = v; if (overlay != null) overlay.rateTouched = System.currentTimeMillis() else touched = System.currentTimeMillis() },
                        onDone = { it.pixelbox.cmwatch.rules.SpeechRate.snap(draft).let { v -> if (v != rate) onRate(v) } },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Bars(still = paused)
                    val openLabel = source?.let { stringResource(R.string.reading_open, it) }
                    if (roomy) Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                            .then(if (onOpen != null) Modifier.handCursor().clickable(onClickLabel = openLabel, onClick = onOpen) else Modifier)
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp),
                    ) {
                        Text(if (paused) stringResource(R.string.reading_paused) else source ?: stringResource(R.string.reading_now), style = MonoSmall, maxLines = 1, overflow = TextOverflow.Clip)
                        Text(ReadingBar.excerpt(text), style = MaterialTheme.typography.bodyMedium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
                    } else Spacer(Modifier.weight(1f))
                    RatePill(rate, onClick = { onRate(it.pixelbox.cmwatch.rules.SpeechRate.next(rate)) }) { setOpen(true) }
                    if (onVoice != null) VoicePill(voice, onVoice)
                }
                if (paused) FilledIconButton(onClick = onResume, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.PlayArrow, stringResource(R.string.reading_resume))
                } else FilledTonalIconButton(onClick = onPause, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Pause, stringResource(R.string.reading_pause), tint = CmColors.actionIcon)
                }
                IconButton(onClick = onStop, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Stop, stringResource(R.string.stop_reading), tint = CmColors.actionIcon)
                }
            }
        }
    }
}

/** Il valore della velocità mentre lo slider è aperto, su fondo pieno: il tocco richiude lo slider. */
@Composable
private fun RateValue(rate: Float, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.handCursor()) {
        Text(rateLabel(rate), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** «1,35×» con la virgola della lingua dell'app. */
@Composable
fun rateLabel(r: Float): String {
    val n = java.text.NumberFormat.getInstance(androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
        .apply { maximumFractionDigits = 2 }.format(r)
    return stringResource(R.string.speech_rate_short, n)
}

/**
 * Lo slider della velocità, da 0,5× a 2× a passi di 0,05, con le scritte 0,5×, 1× e 2× sotto (Franz, 07/10 22:06). Lo usano
 * la barra di lettura e le impostazioni. `onDone` arriva al rilascio: la voce riparte una volta sola, dalla frase in corso.
 */
/** La parte della traccia dopo il cursore, visibile sul fondo della barra (#292F3A) e delle impostazioni. */
private val SLIDER_TRACK = androidx.compose.ui.graphics.Color(0xFF4A5468)

@Composable
fun RateSlider(value: Float, onChange: (Float) -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val r = it.pixelbox.cmwatch.rules.SpeechRate
    val label = rateLabel(value)
    val desc = stringResource(R.string.speech_rate)
    // I gesti si registrano una volta: leggono sempre le funzioni di adesso.
    val change by androidx.compose.runtime.rememberUpdatedState(onChange)
    val done by androidx.compose.runtime.rememberUpdatedState(onDone)
    Column(modifier) {
        // Disegnato qui, come nel mockup approvato: traccia sottile e pallino tondo. Lo slider di Material 3 Expressive ha
        // una traccia spessa, il cursore a stanghetta e la parte dopo il cursore quasi invisibile sul fondo della barra.
        BoxWithConstraints(
            Modifier.fillMaxWidth().height(28.dp)
                .semantics {
                    contentDescription = desc
                    stateDescription = label
                    progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(value, r.MIN..r.MAX)
                    setProgress { v -> change(r.snap(v)); done(); true }
                }
                .pointerInput(Unit) {
                    fun at(x: Float) = r.snap(r.MIN + (x / size.width).coerceIn(0f, 1f) * (r.MAX - r.MIN))
                    detectTapGestures(onTap = { o -> change(at(o.x)); done() })
                }
                .pointerInput(Unit) {
                    fun at(x: Float) = r.snap(r.MIN + (x / size.width).coerceIn(0f, 1f) * (r.MAX - r.MIN))
                    detectHorizontalDragGestures(
                        onDragStart = { o -> change(at(o.x)) },
                        onDragEnd = { done() }, onDragCancel = { done() },
                        onHorizontalDrag = { c, _ -> change(at(c.position.x)) },
                    )
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            val f = r.fraction(value).coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(SLIDER_TRACK))
            Box(Modifier.width(maxWidth * f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(CmColors.actionIcon))
            Box(Modifier.offset(x = maxWidth * r.fraction(r.NORMAL) - 1.dp).width(2.dp).height(14.dp).clip(RoundedCornerShape(1.dp)).background(CmColors.text2))
            Box(Modifier.offset(x = maxWidth * f - 10.dp).size(20.dp).clip(CircleShape).background(CmColors.actionIcon))
        }
        Box(Modifier.fillMaxWidth()) {
            val small = MaterialTheme.typography.labelSmall.copy(color = CmColors.text2)
            Text(rateLabel(r.MIN), style = small, modifier = Modifier.align(Alignment.TopStart))
            Text(rateLabel(r.NORMAL), style = small, modifier = Modifier.align(androidx.compose.ui.BiasAlignment(2 * r.fraction(r.NORMAL) - 1, -1f)))
            Text(rateLabel(r.MAX), style = small, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * Cambia voce durante la lettura (Franz, 04/10 20:28): a ogni tocco la voce italiana dopo, dopo l'ultima di nuovo la
 * predefinita (`VoiceRules.next`); la lettura riparte dal pezzo che stava dicendo, con la voce nuova.
 */
@Composable
private fun VoicePill(voice: String?, onClick: () -> Unit) {
    val desc = stringResource(R.string.voice_change, voice ?: stringResource(R.string.voice_default))
    Surface(onClick = onClick, color = CmColors.surface, shape = CircleShape, modifier = Modifier.handCursor().semantics { contentDescription = desc }) {
        Icon(Icons.Rounded.RecordVoiceOver, null, tint = CmColors.text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).size(20.dp))
    }
}

/** Tre barrette che salgono e scendono mentre legge; ferme con le animazioni spente, basse e grigie in pausa. */
@Composable
internal fun Bars(still: Boolean = false) {
    val off = animationsOff() || still
    val flow = if (off) null else rememberInfiniteTransition(label = "bars")
    Row(Modifier.height(20.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(0, 180, 360).forEach { delay ->
            val h = flow?.let {
                val v by it.animateFloat(0.35f, 1f, infiniteRepeatable(tween(520, delayMillis = delay, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bar$delay")
                v
            } ?: if (still) 0.45f else 0.6f
            Box(Modifier.width(4.dp).fillMaxHeight(h).clip(RoundedCornerShape(2.dp)).background(if (still) CmColors.text2 else CmColors.actionIcon))
        }
    }
}

/** Quanto il dito resta fermo sullo slider prima che la voce riparta con la velocità nuova. */
private const val LIVE_RATE_MS = 300L

/**
 * Il pannello della modalità live (Franz, 08/10 20:03: «un box di controllo proprio come quello audio anche nell'app»):
 * stesso posto e stessa forma del mini-controller della lettura. Le barrette si muovono mentre la live parla in cuffia;
 * sopra «LIVE» col filtro o il perché della pausa, sotto l'ultima cosa detta; poi il filtro al posto della velocità, pausa
 * e spegni. La notizia dopo resta sul watch: un tasto in più lasciava al testo una parola sola.
 */
@Composable
fun LivePill(
    last: String?, speaking: Boolean, paused: Boolean, noHeadset: Boolean, onlyBlocking: Boolean,
    onPause: () -> Unit, onFilter: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = CmColors.surfaceHigh, shadowElevation = 6.dp) {
        BoxWithConstraints {
            val roomy = maxWidth >= 300.dp
            Row(
                Modifier.heightIn(min = 60.dp).padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Bars(still = !speaking || paused)
                if (roomy) Column(Modifier.weight(1f).padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        stringResource(
                            when {
                                paused -> R.string.live_panel_paused
                                onlyBlocking -> R.string.live_panel_blocking
                                else -> R.string.live_panel_all
                            }
                        ),
                        style = MonoSmall, maxLines = 1, overflow = TextOverflow.Clip,
                    )
                    Text(
                        // Senza cuffie la riga sotto dice perché tace, in chiaro: in alto non ci stava.
                        if (noHeadset) stringResource(R.string.live_panel_no_headset) else last?.let { ReadingBar.excerpt(it) } ?: stringResource(R.string.live_panel_quiet),
                        style = MaterialTheme.typography.bodyMedium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip,
                    )
                } else Spacer(Modifier.weight(1f))
                // Il filtro come la pillola della velocità: dice quello che c'è, il tocco passa all'altro.
                val filterLabel = stringResource(if (onlyBlocking) R.string.live_chip_blocking else R.string.live_chip_all)
                val filterDesc = stringResource(if (onlyBlocking) R.string.live_all else R.string.live_only_blocking)
                Surface(onClick = onFilter, color = CmColors.surface, shape = CircleShape, modifier = Modifier.handCursor().semantics { contentDescription = filterDesc }) {
                    Text(filterLabel, style = MaterialTheme.typography.labelLarge, color = CmColors.text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
                if (paused) FilledIconButton(onClick = onPause, modifier = Modifier.size(44.dp), enabled = !noHeadset) {
                    Icon(Icons.Rounded.PlayArrow, stringResource(R.string.reading_resume))
                } else FilledTonalIconButton(onClick = onPause, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Pause, stringResource(R.string.reading_pause), tint = CmColors.actionIcon)
                }
                IconButton(onClick = onStop, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.Stop, stringResource(R.string.live_stop), tint = CmColors.actionIcon)
                }
            }
        }
    }
}
