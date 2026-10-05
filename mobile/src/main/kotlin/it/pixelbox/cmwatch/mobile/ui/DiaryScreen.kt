@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.RecapSessions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.ui.art.EmptyDiaryScene
import it.pixelbox.cmwatch.rules.PhoneOverview
import it.pixelbox.cmwatch.rules.Registro
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.pluralStringResource
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Il Registro (ex Diario; mockup approvato da Franz il 02/10 alle 09:10): blocchi uguali, uno sotto l'altro. Stanotte
 * (coda e «Aggiungi»), la Notte (un lavoro per riga, senza percorsi), i giorni (Oggi aperto, i precedenti chiusi, una riga
 * per progetto), la quota. Lo storico giorno per giorno della quota non c'è: l'app tiene i campioni di poche ore, quindi
 * la quota mostra le barre di adesso e gli avvisi.
 */
@Composable
fun DiaryScreen(
    state: State, quotaEvents: List<Event>, history: List<Event>, nightReport: Event?, ttsMinChars: Int, onSpeak: (String) -> Unit,
    onAdd: () -> Unit, onRemove: (jobId: String) -> Unit,
    rings: List<PhoneOverview.Ring> = emptyList(), onQuadro: () -> Unit = {}, onSession: (String) -> Unit = {},
    today: LocalDate = LocalDate.now(),
    /** Solo per i provini: i giorni (`ref`) già aperti. */
    openDays: Set<String> = emptySet(),
) {
    val recap = state.recap
    val items = state.night.items
    val empty = recap.items.isEmpty() && state.night.queued == 0 && state.night.running == null && quotaEvents.isEmpty() && items.isNullOrEmpty() && history.isEmpty() && nightReport == null
    if (empty) {
        Column(Modifier.fillMaxSize().background(CmColors.bg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            EmptyDiaryScene(Modifier.fillMaxWidth().height(240.dp))
            Text(stringResource(R.string.diary_empty), color = CmColors.text2, style = MaterialTheme.typography.bodyLarge)
            if (items != null) FilledTonalButton(onClick = onAdd, modifier = Modifier.padding(top = 12.dp)) { Text(stringResource(R.string.reg_add)) }
        }
        return
    }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    LazyColumn(Modifier.fillMaxSize().background(CmColors.bg), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "tonight") { Tonight(state, onAdd, onRemove) }
        nightReport?.let { n -> item(key = "night") { NightCard(n, onSpeak) } }
        // Oggi a blocchi per sessione, come i giorni passati e la home (segnalazione 03/10 20:01).
        if (recap.items.isNotEmpty()) item(key = "today") {
            val today = remember(recap) {
                RecapSessions.View("", listOf(RecapSessions.Section(RecapSessions.Kind.OPEN, "", recap.items.map { RecapSessions.Entry(icon = "", name = it.project, text = it.done, next = it.next) }, emptyList())))
            }
            RecapDayCard(stringResource(R.string.reg_today), today, recap.items.joinToString("\n") { "${it.project}: ${it.done}" }, state.sessions, onSession, onSpeak, startOpen = true)
        }
        items(history.filter { it.ref != recap.date }, key = { "h-" + it.key }) { e ->
            val day = runCatching { LocalDate.parse(e.ref) }.getOrNull()
            val title = when {
                day == today -> stringResource(R.string.reg_today)
                day == today.minusDays(1) -> stringResource(R.string.reg_yesterday)
                else -> dateLabel(e.ref.orEmpty(), locale)
            }
            // Segnalazione 03/10 17:52: il recap del plugin diviso per sessione, come nella home; se non si legge, il testo.
            val view = remember(e.body) { RecapSessions.parse(e.body) }
            if (view.sections.any { s -> s.entries.isNotEmpty() }) RecapDayCard(title, view, e.body, state.sessions, onSession, onSpeak, startOpen = e.ref in openDays)
            else DayCard(title, Registro.recap(e.body), e.body, startOpen = false, onSession, onSpeak, ttsMinChars)
        }
        if (rings.isNotEmpty() || quotaEvents.isNotEmpty()) item(key = "quota") {
            RegCard {
                RegLabel(stringResource(R.string.reg_quota))
                QuotaBars(rings, onQuadro, stacked = true)
                quotaEvents.take(5).forEach { e ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(e.title, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Clip)
                        Text(hhmm(e.ts), style = MonoSmall)
                    }
                }
            }
        }
    }
}

/** Stanotte: una riga con «Aggiungi» tonale (il bottone pieno in fondo non c'è più), poi i lavori in coda. */
@Composable
private fun Tonight(state: State, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    val items = state.night.items
    RegCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RegLabel(stringResource(R.string.reg_tonight))
                Text(
                    if (state.night.queued > 0) pluralStringResource(R.plurals.reg_queued, state.night.queued, state.night.queued) else stringResource(R.string.reg_queue_empty),
                    style = MaterialTheme.typography.bodyLarge, color = CmColors.text,
                )
            }
            // Contratto 1.17: senza `items` il relay è precedente e la coda non si tocca dall'app.
            if (items != null) FilledTonalButton(onClick = onAdd) { Text(stringResource(R.string.reg_add)) }
        }
        state.night.running?.let {
            Text(stringResource(R.string.night_running, it), color = CmColors.busy, style = MaterialTheme.typography.bodyMedium)
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), color = CmColors.busy, trackColor = CmColors.briefTrack)
        }
        if (items == null) Text(stringResource(R.string.night_update_pc), color = CmColors.text2, style = MaterialTheme.typography.bodyMedium)
        items?.forEach { job ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(job.name, fontWeight = FontWeight.SemiBold, color = CmColors.text, style = MaterialTheme.typography.bodyMedium)
                    Text(job.prompt, color = CmColors.text2, style = MaterialTheme.typography.bodySmall)
                    if (job.started != null) Text(stringResource(R.string.night_started), color = CmColors.busy, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { onRemove(job.id) }, enabled = job.started == null) { Text(stringResource(R.string.night_remove), color = CmColors.actionIcon) }
            }
        }
    }
}

/** La notte: un lavoro per riga con ✓ o ✗, durata e una frase; ▶ legge il resoconto; il tocco apre il testo intero. */
@Composable
private fun NightCard(n: Event, onSpeak: (String) -> Unit) {
    val jobs = Registro.night(n.body)
    var open by androidx.compose.runtime.saveable.rememberSaveable(n.key) { androidx.compose.runtime.mutableStateOf(false) }
    RegCard(Modifier.handCursor().clickable { open = !open }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { RegLabel(n.title) }
            IconButton(onClick = { onSpeak(n.body) }) { Icon(Icons.Rounded.PlayArrow, stringResource(R.string.fy_btn_listen), tint = CmColors.actionIcon) }
        }
        if (jobs.isEmpty() || open) Text(n.body, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        else jobs.forEachIndexed { i, j ->
            if (i > 0) HorizontalDivider(color = CmColors.line)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(top = 2.dp).size(22.dp).clip(CircleShape).background(if (j.ok) CmColors.briefGoodInk else CmColors.briefAlertInk), contentAlignment = Alignment.Center) {
                    Text(if (j.ok) "✓" else "✗", style = MaterialTheme.typography.labelMedium, color = if (j.ok) CmColors.briefGood else CmColors.briefAlert)
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(j.project, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
                        j.seconds?.let { Text(stringResource(R.string.reg_minutes, Registro.minutes(it)), style = MonoSmall) }
                    }
                    Text(j.text, style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
                }
            }
        }
    }
}

/** Un giorno: titolo e quanti progetti; aperto, una riga per progetto (tocco = la sua sessione), o il testo se non si legge. */
@Composable
private fun DayCard(title: String, lines: List<Registro.Line>, raw: String?, startOpen: Boolean, onSession: (String) -> Unit, onSpeak: (String) -> Unit, ttsMinChars: Int) {
    var open by androidx.compose.runtime.saveable.rememberSaveable(title) { androidx.compose.runtime.mutableStateOf(startOpen) }
    RegCard {
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).handCursor().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = CmColors.text, modifier = Modifier.weight(1f))
            if (lines.isNotEmpty()) Text(pluralStringResource(R.plurals.reg_projects, lines.size, lines.size), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = CmColors.text2)
        }
        if (open) {
            if (lines.isEmpty()) raw?.let { Speakable(it, speak = it.length > ttsMinChars, onSpeak) }
            lines.forEach { l ->
                Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).handCursor().clickable { onSession(l.project) }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(l.project, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.width(128.dp))
                    Text(l.text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * Un giorno dello storico letto per sessione (segnalazione 03/10 17:52): il titolo del recap con ▶, poi le sezioni con
 * l'intestazione nel colore del gruppo, come nella home, e un blocco per sessione: badge (della sessione se è ancora viva,
 * se no dall'emoji del recap: tondo personale, quadrato lavoro), nome, strumento, riassunto, domanda, prossimo passo.
 * Tocco su una sessione viva = la sua scheda.
 */
@Composable
private fun RecapDayCard(
    title: String, view: RecapSessions.View, raw: String, live: List<Session>,
    onSession: (String) -> Unit, onSpeak: (String) -> Unit, startOpen: Boolean = false,
) {
    var open by androidx.compose.runtime.saveable.rememberSaveable(title) { androidx.compose.runtime.mutableStateOf(startOpen) }
    val count = view.sections.sumOf { it.entries.size }
    RegCard {
        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).handCursor().clickable { open = !open }, verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = CmColors.text, modifier = Modifier.weight(1f))
            Text(pluralStringResource(R.plurals.reg_projects, count, count), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
            Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = CmColors.text2)
        }
        if (open) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(view.title, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.weight(1f))
                // Oggi non ha il titolo del recap: resta solo il ▶.
                val reading = LocalSpeaking.current == raw
                if (reading) RatePill()
                androidx.compose.material3.FilledTonalIconButton(onClick = { onSpeak(raw) }, modifier = Modifier.size(36.dp)) {
                    Icon(if (reading) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, stringResource(if (reading) R.string.stop_reading else R.string.read_aloud), tint = CmColors.actionIcon, modifier = Modifier.size(20.dp))
                }
            }
            view.sections.forEach { sec ->
                val tone = when (sec.kind) {
                    RecapSessions.Kind.WAITING -> CmColors.briefWarn
                    RecapSessions.Kind.OPEN -> CmColors.briefRing
                    else -> CmColors.text2
                }
                if (sec.title.isNotBlank()) GroupHeader(sec.title, tone)
                sec.entries.forEach { e -> RecapEntry(e, sec.kind, live.firstOrNull { x -> x.name == e.name && x.state != SessionState.GONE }, onSession) }
                sec.lines.forEach { l -> Text(l, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, modifier = Modifier.padding(horizontal = 4.dp)) }
            }
        }
    }
}

@Composable
private fun RecapEntry(
    e: RecapSessions.Entry, kind: RecapSessions.Kind,
    live: Session?, onSession: (String) -> Unit,
) {
    val personal = RecapSessions.personal(e.icon)
    // Senza la sessione viva il badge si ricava dal recap: colore dall'emoji, forma dall'account, glifo dalla sezione.
    val badge = live ?: Session(
        id = e.name, name = e.name, account = if (personal) "personale" else "lavoro", project = e.name, since = 0,
        state = when (kind) {
            RecapSessions.Kind.WAITING -> SessionState.WAITING
            RecapSessions.Kind.CLOSED -> SessionState.GONE
            else -> SessionState.IDLE
        },
        icon = e.icon, accountKind = if (personal) "personal" else "work",
    )
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CmColors.surface)
            .then(if (live != null) Modifier.handCursor().clickable { onSession(e.name) } else Modifier).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SessionBadge(badge, 22.dp)
            Text(e.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.weight(1f))
            e.tool?.let { Text(it, style = MonoSmall, maxLines = 1, overflow = TextOverflow.Clip) }
        }
        e.text?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2) }
        e.detail?.let { d -> Text(d, style = MaterialTheme.typography.bodyMedium, color = if (kind == RecapSessions.Kind.WAITING) CmColors.text else CmColors.text2) }
        e.next?.let { Text("↳ $it", style = MaterialTheme.typography.bodyMedium, color = CmColors.actionIcon) }
    }
}

/** Il blocco del Registro: fondo basso, angoli ampi, come nel mockup. */
@Composable
private fun RegCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).smoothSize().then(modifier).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), content = content,
    )

@Composable
private fun RegLabel(text: String) = Text(text.uppercase(), style = MonoSmall.copy(color = CmColors.briefLabel, letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)))

private val HM = DateTimeFormatter.ofPattern("HH:mm")
private fun hhmm(epoch: Long) = HM.format(java.time.Instant.ofEpochSecond(epoch).atZone(java.time.ZoneId.systemDefault()))

/** Nella lingua del telefono, non in quella della JVM (negli snapshot usciva in inglese). */
private fun dateLabel(iso: String, locale: java.util.Locale): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)) }.getOrDefault(iso)
