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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material3.Icon
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
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.SettingsDevices
import it.pixelbox.cmwatch.rules.SettingsDevices.Tone
import it.pixelbox.cmwatch.ui.tokens.CmColors

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
            Wire(m.pcLink, reverse = false, Modifier.weight(1f).padding(top = 35.dp))
            Node(
                Icons.Rounded.Computer, 84.dp, m.pc.tone, m.paired && selected == DeviceNode.PC, dashed = !m.paired,
                m.pc.host ?: stringResource(R.string.dev_pc),
                when {
                    !m.paired -> ""
                    m.pc.ageMinutes == null -> stringResource(R.string.dev_updated_now)
                    else -> stringResource(R.string.dev_updated_ago, m.pc.ageMinutes!!)
                },
                enabled = m.paired, lift = 6.dp,
            ) { onSelect(DeviceNode.PC) }
            Wire(m.watchLink, reverse = true, Modifier.weight(1f).padding(top = 35.dp))
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
    enabled: Boolean, lift: Dp = 0.dp, onClick: () -> Unit,
) {
    val desc = stringResource(R.string.dev_details, name)
    Column(Modifier.width(size + 14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.offset(y = -lift).size(size)) {
            val ring = if (selected) CmColors.actionIcon else CmColors.line
            Box(
                Modifier.fillMaxSize().clip(CircleShape)
                    .background(if (dashed) Color.Transparent else CmColors.surface)
                    .then(
                        if (dashed) Modifier.drawBehind {
                            val w = 2.dp.toPx()
                            drawCircle(CmColors.briefTrack, radius = this.size.minDimension / 2 - w / 2, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
                        } else Modifier.border(2.dp, ring, CircleShape),
                    )
                    .then(if (enabled) Modifier.clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = desc } else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = if (dashed) CmColors.stale else CmColors.actionIcon, modifier = Modifier.size(size * 0.42f))
            }
            if (!dashed) Box(
                Modifier.align(Alignment.TopEnd).offset(x = (-3).dp, y = 3.dp).size(14.dp).clip(CircleShape)
                    .background(CmColors.surfaceLow).padding(3.dp).clip(CircleShape).background(toneColor(tone)),
            )
        }
        Text(name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium), color = if (dashed) CmColors.text2 else CmColors.text, maxLines = 1)
        if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.labelMedium, color = CmColors.text2, maxLines = 1)
    }
}

/** Il filo fra due dispositivi: pieno nel colore dello stato, tratteggiato se manca l'abbinamento; sul vivo scorre un punto. */
@Composable
private fun Wire(tone: Tone, reverse: Boolean, modifier: Modifier) {
    val color = toneColor(tone)
    val moving = tone == Tone.LIVE && !animationsOff()
    val t = if (moving) {
        val flow = rememberInfiniteTransition(label = "wire")
        val v by flow.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart), label = "dot")
        v
    } else null
    Canvas(modifier.height(8.dp)) {
        val y = size.height / 2
        val w = 2.dp.toPx()
        if (tone == Tone.OFF) drawLine(color, Offset(0f, y), Offset(size.width, y), w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
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
                val q = a.pct?.let { stringResource(R.string.quota_h5_short, it) } ?: "–"
                add(a.account to if (a.stale) stringResource(R.string.fact_quota_stale, q) else q)
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
