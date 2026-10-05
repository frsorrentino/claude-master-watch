package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.LaptopChromebook
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SettingsDevices
import it.pixelbox.cmwatch.rules.SettingsDevices.Tone
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** La fascia dei cerchi: i nomi sotto stanno allineati anche se il PC è più grande. */
private val NODE_BAND = 72.dp

/** Il dispositivo scelto nello schema: la sua scheda si apre sotto. */
enum class DeviceNode { PHONE, PC, WATCH }

private fun toneColor(t: Tone): Color = when (t) {
    Tone.LIVE -> CmColors.briefGood
    Tone.STALE -> CmColors.waiting
    Tone.OFF -> CmColors.briefTrack
}

/**
 * Lo schema dei dispositivi (mockup A, Franz 03/10 21:59): telefono, PC e orologio in fila, uniti da fili del colore del
 * loro stato; sul filo vivo scorre un punto, fermo con le animazioni spente. Toccato un dispositivo, sotto la sua scheda.
 * Senza abbinamento PC e orologio sono tratteggiati e al posto della scheda c'è `unpaired`.
 */
@Composable
fun DeviceScheme(m: SettingsDevices.Model, selected: DeviceNode, onSelect: (DeviceNode) -> Unit, unpaired: @Composable () -> Unit = {}) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CmColors.surfaceLow).dotGrid()
            .padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Node(
                Icons.Rounded.PhoneAndroid, 72.dp, m.phone.tone, m.paired && selected == DeviceNode.PHONE, dashed = false,
                stringResource(R.string.dev_phone), stringResource(R.string.dev_this), enabled = m.paired,
            ) { onSelect(DeviceNode.PHONE) }
            Wire(m.pcLink, reverse = false, Modifier.weight(1f).padding(top = 32.dp))
            Node(
                Icons.Rounded.Computer, 84.dp, m.pc.tone, m.paired && selected == DeviceNode.PC, dashed = !m.paired,
                m.pc.host ?: stringResource(R.string.dev_pc),
                when {
                    !m.paired -> ""
                    m.pc.ageMinutes == null -> stringResource(R.string.dev_updated_now)
                    else -> stringResource(R.string.dev_updated_ago, m.pc.ageMinutes!!)
                },
                enabled = m.paired,
            ) { onSelect(DeviceNode.PC) }
            Wire(m.watchLink, reverse = true, Modifier.weight(1f).padding(top = 32.dp))
            Node(
                Icons.Rounded.Watch, 72.dp, m.watch.tone, m.paired && selected == DeviceNode.WATCH, dashed = m.watch.name == null,
                stringResource(R.string.dev_watch), watchShort(m.watch), enabled = m.paired,
            ) { onSelect(DeviceNode.WATCH) }
        }
        if (m.paired) {
            DeviceCard(m, selected)
            Text(
                stringResource(R.string.dev_hint), style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        } else unpaired()
    }
}

@Composable
private fun watchShort(w: SettingsDevices.Watch): String = stringResource(
    when {
        w.name == null -> R.string.dev_watch_none
        !w.keyDelivered -> R.string.dev_watch_waiting
        w.reachable == false -> R.string.dev_watch_far
        w.reachable == null -> R.string.dev_watch_checking
        else -> R.string.dev_watch_near
    },
)

@Composable
private fun Node(
    icon: ImageVector, size: Dp, tone: Tone, selected: Boolean, dashed: Boolean, name: String, status: String,
    enabled: Boolean, onClick: () -> Unit,
) = SchemeNode(
    icon, size, toneColor(tone), if (selected) CmColors.actionIcon else CmColors.line, dashed, name, status,
    stringResource(R.string.dev_details, name), if (enabled) onClick else null,
)

/**
 * Un dispositivo dello schema, nelle Impostazioni e mentre si accoppia: il cerchio con l'icona, il puntino del suo stato
 * (`dot`), il nome e lo stato sotto; tratteggiato quando non c'è ancora. Toccabile solo con `onClick`.
 */
@Composable
internal fun SchemeNode(
    icon: ImageVector, size: Dp, dot: Color, ring: Color, dashed: Boolean, name: String, status: String,
    desc: String?, onClick: (() -> Unit)?, labelWidth: Dp? = null,
    /** Con molti dispositivi in fila: nome e stato più piccoli, così non si tagliano. */
    compact: Boolean = false,
) {
    // Tutti i cerchi stanno in una fascia alta 72 dp: quello più grande del PC la sborda sopra e sotto, così i nomi restano
    // sulla stessa riga e i fili arrivano al centro di ogni cerchio.
    Column(Modifier.width(labelWidth ?: (size + 14.dp)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.height(NODE_BAND), contentAlignment = Alignment.Center) { Box(Modifier.requiredSize(size)) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape)
                    .background(if (dashed) Color.Transparent else CmColors.surface)
                    .then(
                        if (dashed) Modifier.drawBehind {
                            val w = 2.dp.toPx()
                            drawCircle(CmColors.briefTrack, radius = this.size.minDimension / 2 - w / 2, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                        } else Modifier.border(2.dp, ring, CircleShape),
                    )
                    .then(if (onClick != null) Modifier.handCursor().clickable(role = Role.Button, onClick = onClick).semantics { desc?.let { contentDescription = it } } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = if (dashed) CmColors.stale else CmColors.actionIcon, modifier = Modifier.size(size * 0.42f))
            }
            if (!dashed) Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-3).dp, y = 3.dp).size(14.dp).clip(CircleShape)
                    .background(CmColors.surfaceLow).padding(3.dp).clip(CircleShape).background(dot),
            )
        } }
        val nameColor = if (dashed) CmColors.text2 else CmColors.text
        if (compact) {
            // In fila stretta un nome lungo («Chromebook») scende di corpo finché entra, invece di tagliarsi.
            val nameStyle = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
            val statusStyle = MaterialTheme.typography.labelSmall
            BasicText(
                name, style = LocalTextStyle.current.merge(nameStyle).copy(color = nameColor), maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = nameStyle.fontSize, stepSize = 0.5.sp),
            )
            if (status.isNotEmpty()) BasicText(
                status, style = LocalTextStyle.current.merge(statusStyle).copy(color = CmColors.text2), maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = statusStyle.fontSize, stepSize = 0.5.sp),
            )
        } else {
            Text(name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = nameColor, maxLines = 1)
            if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.labelMedium, color = CmColors.text2, maxLines = 1)
        }
    }
}

/** Il filo fra due dispositivi: pieno nel colore dello stato, tratteggiato se manca l'abbinamento; sul vivo scorre un punto. */
@Composable
private fun Wire(tone: Tone, reverse: Boolean, modifier: Modifier) =
    SchemeWire(toneColor(tone), dashed = tone == Tone.OFF, moving = tone == Tone.LIVE, reverse = reverse, modifier = modifier)

/** Il filo dello schema: pieno o tratteggiato, con il punto che scorre se `moving` (fermo con le animazioni spente). */
@Composable
internal fun SchemeWire(color: Color, dashed: Boolean, moving: Boolean, reverse: Boolean, modifier: Modifier) {
    val run = moving && !animationsOff()
    val t = if (run) {
        val flow = rememberInfiniteTransition(label = "wire")
        val v by flow.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "dot")
        v
    } else null
    Canvas(modifier.height(8.dp)) {
        val y = size.height / 2
        val w = 2.dp.toPx()
        if (dashed) drawLine(color, Offset(0f, y), Offset(size.width, y), w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        else drawLine(color, Offset(0f, y), Offset(size.width, y), w)
        t?.let { f ->
            val x = (if (reverse) 1f - f else f) * size.width
            drawCircle(color.copy(alpha = 0.35f), radius = 7.dp.toPx(), center = Offset(x, y))
            drawCircle(color, radius = 4.dp.toPx(), center = Offset(x, y))
        }
    }
}

/** La scheda del dispositivo scelto: nome, stato in una pillola e i fatti, chiave a sinistra e valore a destra. */
@Composable
private fun DeviceCard(m: SettingsDevices.Model, selected: DeviceNode) {
    val (title, chip, tone) = when (selected) {
        DeviceNode.PC -> Triple(
            m.pc.host ?: stringResource(R.string.dev_pc),
            stringResource(if (m.pc.tone == Tone.LIVE) R.string.chip_connected else R.string.chip_stale), m.pc.tone,
        )
        DeviceNode.PHONE -> Triple(m.phone.model, stringResource(R.string.chip_this_phone), m.phone.tone)
        DeviceNode.WATCH -> Triple(
            m.watch.name ?: stringResource(R.string.dev_watch),
            stringResource(
                when {
                    m.watch.name == null -> R.string.chip_not_paired
                    !m.watch.keyDelivered -> R.string.chip_key_waiting
                    m.watch.reachable == false -> R.string.chip_unreachable
                    else -> R.string.chip_reachable
                },
            ),
            m.watch.tone,
        )
    }
    val facts: List<Pair<String, String>> = when (selected) {
        DeviceNode.PC -> buildList {
            add(stringResource(R.string.fact_updated) to (m.pc.ageMinutes?.let { stringResource(R.string.dev_updated_ago, it) } ?: stringResource(R.string.fact_updated_now)))
            add(stringResource(R.string.fact_open) to m.pc.open.toString())
            m.pc.accounts.forEach { a ->
                val q = a.pct?.let { stringResource(R.string.quota_h5_short, it) }
                add(a.account to when {
                    q == null -> stringResource(if (a.stale) R.string.chip_stale else R.string.fact_quota_none)
                    a.stale -> stringResource(R.string.fact_quota_stale, q)
                    else -> q
                })
            }
            add(stringResource(R.string.fact_channel) to stringResource(R.string.fact_channel_value))
        }
        DeviceNode.PHONE -> listOf(
            stringResource(R.string.fact_app) to stringResource(R.string.fact_app_value, m.phone.version),
            stringResource(R.string.notifications) to stringResource(if (m.phone.notifications) R.string.fact_on else R.string.fact_off),
            stringResource(R.string.fact_paired_to) to (m.pc.host ?: "–"),
        )
        DeviceNode.WATCH -> if (m.watch.name == null) emptyList() else listOf(
            stringResource(R.string.fact_key) to stringResource(if (m.watch.keyDelivered) R.string.fact_key_ok else R.string.fact_key_wait),
            stringResource(R.string.fact_reach) to stringResource(
                when (m.watch.reachable) { true -> R.string.fact_reach_yes; false -> R.string.fact_reach_no; null -> R.string.fact_reach_unknown },
            ),
            stringResource(R.string.fact_via) to stringResource(R.string.fact_via_phone),
        )
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.weight(1f), maxLines = 1)
            Text(
                chip, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = toneColor(tone),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(toneColor(tone).copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        if (selected == DeviceNode.WATCH && m.watch.name == null) Text(stringResource(R.string.watch_none_hint), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        facts.forEach { (k, v) ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                Text(v, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * Lo schema dei collegamenti con i dispositivi veri (contratto 1.32, variante B scelta da Franz il 04/10 alle 17:11): il PC
 * sopra, i dispositivi accoppiati in fila sotto, ognuno appeso al PC con il filo nel colore della sua ultima lettura;
 * quello che si sta usando ha il bordo azzurro e dice «questo». Toccato un dispositivo (o il PC), sotto la sua scheda.
 */
@Composable
fun DeviceSchemeB(m: SettingsDevices.Model, linked: List<SettingsDevices.Linked>, now: Long, selected: String, onSelect: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CmColors.surfaceLow).dotGrid()
            .padding(start = 10.dp, end = 10.dp, top = 20.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SchemeNode(
                Icons.Rounded.Computer, 84.dp, toneColor(m.pc.tone), if (selected == PC_KEY) CmColors.actionIcon else CmColors.line, dashed = false,
                m.pc.host ?: stringResource(R.string.dev_pc),
                listOf(
                    m.pc.ageMinutes?.let { stringResource(R.string.dev_updated_ago, it) } ?: stringResource(R.string.dev_updated_now),
                    pluralStringResource(R.plurals.dev_count, linked.size, linked.size),
                ).joinToString(" · "),
                stringResource(R.string.dev_details, m.pc.host ?: stringResource(R.string.dev_pc)), { onSelect(PC_KEY) }, labelWidth = 260.dp,
            )
        }
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
            val n = linked.size.coerceAtLeast(1)
            val col = maxWidth / n
            val node = minOf(60.dp, col - 18.dp)
            // Il filo dal PC, la sbarra e le discese fino al bordo del cerchio: ognuna nel colore del suo dispositivo,
            // tratteggiata se non ha mai letto. I cerchi, pieni, stanno sopra la fine del filo.
            Canvas(Modifier.fillMaxWidth().height(26.dp + (NODE_BAND - node) / 2 + 2.dp)) {
                val w = 2.dp.toPx()
                val bar = 12.dp.toPx()
                val c = col.toPx()
                val any = linked.any { it.tone != Tone.OFF }
                val trunk = if (any) toneColor(Tone.LIVE) else toneColor(Tone.OFF)
                drawLine(trunk, Offset(size.width / 2, 0f), Offset(size.width / 2, bar), w)
                if (linked.size > 1) drawLine(trunk, Offset(c / 2, bar), Offset(c * (linked.size - 0.5f), bar), w)
                linked.forEachIndexed { i, d ->
                    val x = c * (i + 0.5f)
                    val dash = if (d.tone == Tone.OFF) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
                    drawLine(toneColor(d.tone), Offset(x, bar), Offset(x, size.height), w, pathEffect = dash)
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 26.dp)) {
                linked.forEach { d ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                        SchemeNode(
                            kindIcon(d.kind), node, toneColor(d.tone),
                            if (d.self || selected == d.uid) CmColors.actionIcon else CmColors.line, dashed = d.tone == Tone.OFF,
                            stringResource(kindLabel(d.kind)), if (d.self) stringResource(R.string.dev_this) else seenShort(d.seen, now),
                            stringResource(R.string.dev_details, d.name), { onSelect(d.uid) }, labelWidth = col, compact = col < 84.dp,
                        )
                    }
                }
            }
        }
        val pick = linked.firstOrNull { it.uid == selected }
        if (pick == null) DeviceCard(m, DeviceNode.PC) else LinkedCard(pick, m.pc.host, now)
        Text(stringResource(R.string.dev_hint), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

/** La chiave del PC nella scelta dello schema B (gli altri sono gli uid dei dispositivi). */
const val PC_KEY = "pc"

private fun kindIcon(kind: String?): ImageVector = when (kind) {
    "phone" -> Icons.Rounded.PhoneAndroid
    "watch" -> Icons.Rounded.Watch
    "tablet" -> Icons.Rounded.TabletAndroid
    "chromebook" -> Icons.Rounded.LaptopChromebook
    else -> Icons.Rounded.Devices
}

private fun kindLabel(kind: String?): Int = when (kind) {
    "phone" -> R.string.dev_phone
    "watch" -> R.string.dev_watch
    "tablet" -> R.string.dev_tablet
    "chromebook" -> R.string.dev_chromebook
    else -> R.string.dev_other
}

/** L'ultima lettura in breve sotto il cerchio: ora, minuti, ore, giorni; «mai» se non è mai arrivata. */
@Composable
private fun seenShort(seen: Long?, now: Long): String {
    val s = seen?.let { (now - it).coerceAtLeast(0) } ?: return stringResource(R.string.dev_seen_never)
    return when {
        s < 120 -> stringResource(R.string.dev_seen_now)
        s < 3600 -> stringResource(R.string.dev_seen_min, s / 60)
        s < 86_400 -> stringResource(R.string.dev_seen_h, s / 3600)
        else -> stringResource(R.string.dev_seen_d, s / 86_400)
    }
}

/** La scheda di un dispositivo dello schema B: il nome vero, il tipo, l'ultima lettura, a quale PC. */
@Composable
private fun LinkedCard(d: SettingsDevices.Linked, host: String?, now: Long) {
    val chip = when {
        d.self -> stringResource(R.string.dev_this)
        d.seen == null -> stringResource(R.string.dev_seen_never_long)
        else -> stringResource(R.string.dev_seen_ago, seenShort(d.seen, now))
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(d.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.weight(1f))
            Text(
                chip, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = toneColor(d.tone),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(toneColor(d.tone).copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        listOf(
            stringResource(R.string.fact_kind) to stringResource(kindLabel(d.kind)),
            stringResource(R.string.fact_seen) to (d.seen?.let { hhmmDay(it) } ?: stringResource(R.string.dev_seen_never_long)),
            stringResource(R.string.fact_paired_to) to (host ?: "–"),
        ).forEach { (k, v) ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                Text(v, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun hhmmDay(epoch: Long): String =
    java.time.format.DateTimeFormatter.ofPattern("EEE d\u00A0HH:mm", androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
        .format(java.time.Instant.ofEpochSecond(epoch).atZone(java.time.ZoneId.systemDefault()))

