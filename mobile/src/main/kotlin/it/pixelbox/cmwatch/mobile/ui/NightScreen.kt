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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ExpandMore
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
        // Sezioni richiudibili (Franz, 08/10 12:30): «Progetti» parte chiusa, le altre aperte.
        var needsOpen by rememberSaveable { mutableStateOf(true) }
        var itemsOpen by rememberSaveable { mutableStateOf(true) }
        var projectsOpen by rememberSaveable { mutableStateOf(false) }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "window") {
                Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(if (page.fromLastMessage) R.string.night_window else R.string.night_window_plain, t(page.start), t(page.end), longDuration(page.windowS)),
                        style = MaterialTheme.typography.bodyLarge, color = CmColors.text2,
                    )
                    CountChips(page.counts)
                }
            }
            if (page.needs.isNotEmpty()) {
                item(key = "needs") { Section(stringResource(R.string.night_needs_title, page.needs.size), stringResource(R.string.night_needs_expl), needsOpen) { needsOpen = !needsOpen } }
                if (needsOpen) page.needs.forEachIndexed { i, n -> item(key = "need$i") { NeedRow(n, first = i == 0, onAnswer, onApprove, onSend) } }
            }
            item(key = "items") { Section(stringResource(R.string.night_items_title, page.cards.size), stringResource(R.string.night_items_expl), itemsOpen) { itemsOpen = !itemsOpen } }
            if (itemsOpen) items(page.cards, key = { "c" + it.id }) { c ->
                val times = if (c.end == null) stringResource(R.string.night_since, t(c.start)) else "${t(c.start)}–${t(c.end!!)} · ${shortDuration(c.durationS ?: 0)}"
                CardRow(c, open = open == c.id, times = times, t = ::t, onToggle = { open = if (open == c.id) null else c.id }, onChat = onChat)
            }
            if (page.projects.isNotEmpty()) {
                item(key = "projects") { Section(stringResource(R.string.night_projects_title, page.projects.size), stringResource(R.string.night_projects_expl), projectsOpen) { projectsOpen = !projectsOpen } }
                if (projectsOpen) items(page.projects, key = { "p" + it.name }) { p -> ProjectRow(p) }
            }
        }
    }
}

/** Titolo di sezione che si apre e si chiude al tocco, con una riga che dice cosa c'è dentro. */
@Composable
private fun Section(title: String, explain: String, open: Boolean, onToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth().handCursor().clickable(onClick = onToggle).padding(start = 8.dp, end = 8.dp, top = 14.dp, bottom = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(if (open) Icons.Rounded.ExpandMore else Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = CmColors.actionIcon, modifier = Modifier.size(18.dp))
            Text(title.uppercase(), style = MonoSmall.copy(color = CmColors.text2, letterSpacing = 1.8.sp))
        }
        Text(explain, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
    }
}

/** «✓ 5 finiti · ▶ 1 in corso · ✗ 1 fermi»: solo gli esiti presenti. */
@Composable
private fun CountChips(c: NightPage.Counts) {
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (c.done > 0) CountChip(pluralStringResource(R.plurals.night_c_done, c.done, c.done), CmColors.briefGood)
        if (c.running > 0) CountChip(pluralStringResource(R.plurals.night_c_running, c.running, c.running), CmColors.actionIcon)
        if (c.stopped > 0) CountChip(pluralStringResource(R.plurals.night_c_stopped, c.stopped, c.stopped), CmColors.goneDim)
        if (c.asking > 0) CountChip(pluralStringResource(R.plurals.night_c_asking, c.asking, c.asking), CmColors.advice)
    }
}

@Composable
private fun CountChip(text: String, tone: Color) = Text(
    text, style = MaterialTheme.typography.labelLarge, color = tone,
    modifier = Modifier.clip(CircleShape).background(CmColors.surfaceHigh).padding(horizontal = 12.dp, vertical = 6.dp),
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

private fun iconColor(i: NightPage.Icon): Color = when (i) {
    NightPage.Icon.OK -> CmColors.briefGood
    NightPage.Icon.STOPPED -> CmColors.goneDim
    NightPage.Icon.RUNNING -> CmColors.actionIcon
    NightPage.Icon.QUESTION -> CmColors.advice
}

/**
 * Una sessione o un lavoro della coda: nome, stato a parole, tipo e orari, l'esito intero, i conteggi. Al tocco i passi e
 * la conversazione (Franz, 08/10 12:30: le barre sull'asse non si capivano, tolte).
 */
@Composable
private fun CardRow(c: NightPage.Card, open: Boolean, times: String, t: (Long) -> String, onToggle: () -> Unit, onChat: (String) -> Unit) {
    val tone = iconColor(c.icon)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(CmColors.surface).handCursor().clickable(onClick = onToggle)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(c.title, style = MaterialTheme.typography.titleMedium, color = CmColors.text, modifier = Modifier.weight(1f))
            Text(
                stringResource(when (c.icon) {
                    NightPage.Icon.OK -> R.string.night_st_ok; NightPage.Icon.RUNNING -> R.string.night_st_running
                    NightPage.Icon.STOPPED -> R.string.night_st_stopped; NightPage.Icon.QUESTION -> R.string.night_st_asking
                }),
                style = MaterialTheme.typography.labelLarge, color = tone,
                modifier = Modifier.clip(CircleShape).background(tone.copy(alpha = .16f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(stringResource(if (c.queue) R.string.night_kind_job else R.string.night_kind_session) + " · " + times, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        c.folder?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2) }
        c.detail?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = CmColors.text) }
        if (c.commits != null) Text(
            listOf(stringResource(R.string.night_count_commits, c.commits!!), stringResource(R.string.night_count_tests, c.tests ?: 0), stringResource(R.string.night_count_prompts, c.prompts ?: 0)).joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
        )
        if (open) {
            if (c.steps.isNotEmpty()) Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
