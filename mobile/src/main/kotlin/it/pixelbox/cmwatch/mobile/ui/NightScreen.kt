package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.NightPage
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * La pagina «Notte» (specifica docs/proposte/2026-10-07-pagina-notte.md, approvata da Franz il 07/10 alle 21:50): la
 * finestra della notte, quello che serve a te, una card per lavoro o sessione che si apre al tocco (e chiude quella aperta
 * prima), i progetti. Un solo tasto pieno: il primo di «Serve a te». `opened`: la card aperta all'inizio (test).
 */
@Composable
fun NightScreen(
    page: NightPage.Page?, error: String?, loading: Boolean, onBack: () -> Unit, onRefresh: () -> Unit,
    onChat: (String) -> Unit, onAnswer: (String, Int) -> Unit, onApprove: (String) -> Unit, onSend: (String, String) -> Unit,
    zone: ZoneId = ZoneId.systemDefault(), opened: String? = null,
) {
    val hm = remember(zone) { DateTimeFormatter.ofPattern("HH:mm").withZone(zone) }
    fun t(s: Long) = hm.format(Instant.ofEpochSecond(s))
    var open by rememberSaveable { mutableStateOf(opened) }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = CmColors.text) }
            val title = page?.let { p -> stringResource(R.string.night_title, p.dayBefore.dayOfMonth, p.day.dayOfMonth, p.day.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)) }
                ?: stringResource(R.string.night_menu)
            Text(title, style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh, enabled = !loading) { Icon(Icons.Rounded.Refresh, stringResource(R.string.night_refresh), tint = CmColors.actionIcon) }
        }
        if (loading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = CmColors.actionIcon)
        if (page == null) {
            Text(error ?: stringResource(R.string.night_loading), color = CmColors.text2, modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp))
            return@Column
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "window") {
                Text(
                    stringResource(if (page.fromLastMessage) R.string.night_window else R.string.night_window_plain, t(page.start), t(page.end), longDuration(page.windowS)),
                    style = MaterialTheme.typography.bodyLarge, color = CmColors.text2, modifier = Modifier.padding(start = 44.dp, end = 12.dp),
                )
            }
            if (page.needs.isNotEmpty()) {
                item(key = "needs") { Section(stringResource(R.string.night_needs)) }
                page.needs.forEachIndexed { i, n -> item(key = "need$i") { NeedRow(n, first = i == 0, onAnswer, onApprove, onSend) } }
            }
            item(key = "items") { Section(stringResource(R.string.night_items)) }
            item(key = "axis") { Axis(page.axis) }
            items(page.cards, key = { "c" + it.id }) { c ->
                CardRow(c, open = open == c.id, times = if (c.end == null) stringResource(R.string.night_since, t(c.start)) else "${t(c.start)}–${t(c.end!!)} · ${shortDuration(c.durationS ?: 0)}", t = ::t,
                    onToggle = { open = if (open == c.id) null else c.id }, onChat = onChat)
            }
            if (page.projects.isNotEmpty()) {
                item(key = "projects") { Section(stringResource(R.string.night_projects)) }
                items(page.projects, key = { "p" + it.name }) { p -> ProjectRow(p) }
            }
        }
    }
}

@Composable
private fun Section(text: String) = Text(
    text.uppercase(), style = MonoSmall.copy(color = CmColors.text2, letterSpacing = 1.8.sp), modifier = Modifier.padding(start = 8.dp, top = 14.dp, bottom = 2.dp),
)

/** «2 ore e 3 minuti»: la durata della finestra. */
@Composable
private fun longDuration(s: Int): String {
    val h = s / 3600; val m = (s % 3600) / 60
    val hs = pluralStringResource(R.plurals.night_hours, h, h); val ms = pluralStringResource(R.plurals.night_minutes, m, m)
    return when { h > 0 && m > 0 -> stringResource(R.string.night_and, hs, ms); h > 0 -> hs; else -> ms }
}

/** «1 h 4 min», «20 min»: la durata di una card. */
@Composable
private fun shortDuration(s: Int): String {
    val h = s / 3600; val m = ((s % 3600) + 30) / 60
    return when {
        h == 0 -> stringResource(R.string.night_short_min, maxOf(1, m))
        m == 0 -> stringResource(R.string.night_short_h, h)
        else -> stringResource(R.string.night_short_hm, h, m)
    }
}

@Composable
private fun NeedRow(n: NightPage.Need, first: Boolean, onAnswer: (String, Int) -> Unit, onApprove: (String) -> Unit, onSend: (String, String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CmColors.surface).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(CmColors.surfaceHigh), contentAlignment = Alignment.Center) {
            Text(if (n.kind == NightPage.NeedKind.QUESTION) "?" else "!", color = if (n.kind == NightPage.NeedKind.QUESTION) CmColors.advice else CmColors.waiting, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(n.session, style = MaterialTheme.typography.titleMedium, color = CmColors.text)
            Text(n.text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        }
        when (n.kind) {
            NightPage.NeedKind.QUESTION -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                n.options.forEachIndexed { k, o -> NeedButton("${o.n} · ${o.label}", filled = first && k == 0) { onAnswer(n.session, o.n) } }
            }
            NightPage.NeedKind.APPROVAL -> NeedButton(stringResource(R.string.night_approve), filled = first) { n.task?.let(onApprove) }
            NightPage.NeedKind.UNBLOCK -> NeedButton(stringResource(R.string.night_send, n.text), filled = first) { onSend(n.session, n.text) }
        }
    }
}

@Composable
private fun NeedButton(label: String, filled: Boolean, onClick: () -> Unit) {
    if (filled) Button(onClick = onClick) { Text(label) } else FilledTonalButton(onClick = onClick) { Text(label) }
}

/** Le ore piene e mezze sopra le card, allineate alle barre (che stanno dopo l'icona). */
@Composable
private fun Axis(ticks: List<NightPage.Tick>) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp).padding(start = 66.dp, end = 48.dp)) {
        ticks.forEach { tk ->
            Text(tk.label, style = MonoSmall.copy(color = CmColors.text2), modifier = Modifier.offset(x = maxWidth * tk.at.toFloat() - 18.dp))
        }
    }
}

private fun iconColor(i: NightPage.Icon): Color = when (i) {
    NightPage.Icon.OK -> CmColors.briefGood
    NightPage.Icon.STOPPED -> CmColors.goneDim
    NightPage.Icon.RUNNING -> CmColors.actionIcon
    NightPage.Icon.QUESTION -> CmColors.advice
}

@Composable
private fun CardRow(c: NightPage.Card, open: Boolean, times: String, t: (Long) -> String, onToggle: () -> Unit, onChat: (String) -> Unit) {
    val tone = iconColor(c.icon)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CmColors.surface)) {
        Row(
            Modifier.fillMaxWidth().handCursor().clickable(onClick = onToggle).padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(CmColors.surfaceHigh), contentAlignment = Alignment.Center) {
                when (c.icon) {
                    NightPage.Icon.OK -> Icon(Icons.Rounded.Check, null, tint = tone)
                    NightPage.Icon.STOPPED -> Icon(Icons.Rounded.Close, null, tint = tone)
                    NightPage.Icon.RUNNING -> Icon(Icons.Rounded.PlayArrow, null, tint = tone)
                    NightPage.Icon.QUESTION -> Text("?", color = tone, fontWeight = FontWeight.Bold)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, color = CmColors.text, modifier = Modifier.weight(1f, fill = false))
                    if (c.queue) Text(
                        stringResource(R.string.night_queue_tag), style = MonoSmall.copy(color = CmColors.advice, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.clip(CircleShape).background(CmColors.advice.copy(alpha = .16f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Text(times, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                if ((open || c.detail == null) && c.folder != null) Text(c.folder!!, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
                if (!open && c.detail != null) Text(c.detail!!, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, maxLines = 2)
                BoxWithConstraints(Modifier.padding(top = 8.dp).fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(CmColors.surfaceHigh)) {
                    Box(Modifier.offset(x = maxWidth * c.from.toFloat()).width(maxOf(5.dp, maxWidth * (c.to - c.from).toFloat())).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(tone))
                }
            }
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = CmColors.text2)
        }
        if (open) Column(Modifier.padding(start = 68.dp, end = 18.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            c.detail?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = CmColors.text) }
            if (c.commits != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.night_count_commits, c.commits!!))
                Chip(stringResource(R.string.night_count_tests, c.tests ?: 0))
                Chip(stringResource(R.string.night_count_prompts, c.prompts ?: 0))
            }
            if (c.steps.isNotEmpty()) Column(Modifier.padding(start = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                c.steps.forEach { s -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t(s.at), style = MonoSmall.copy(color = CmColors.text2))
                    Text(s.text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text)
                } }
            }
            c.chat?.let { name ->
                FilledTonalButton(onClick = { onChat(name) }) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.night_conversation))
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) = Text(
    text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(CmColors.surfaceHigh).padding(horizontal = 12.dp, vertical = 6.dp),
)

@Composable
private fun ProjectRow(p: NightPage.Project) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CmColors.surface).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.name, style = MaterialTheme.typography.titleMedium, color = CmColors.text, modifier = Modifier.weight(1f))
            Text(if (p.total > 0) stringResource(R.string.night_parts, p.done, p.total) else stringResource(R.string.night_no_parts), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        }
        if (p.parts.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            p.parts.forEach { st ->
                Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(3.dp)).background(when (st) { "done" -> CmColors.briefGood; "doing" -> CmColors.actionIcon; else -> CmColors.surfaceHigh }))
            }
        }
        if (p.waiting.isNotEmpty()) Text(stringResource(R.string.night_waiting, p.waiting.joinToString(" · ")), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        p.next?.let { Text(stringResource(R.string.night_next, it), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2) }
    }
}
