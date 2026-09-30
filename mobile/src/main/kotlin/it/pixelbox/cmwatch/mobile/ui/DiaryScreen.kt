@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Scheda Diario (design 29/09, schermata 5) con i dati del contratto di oggi: diario, notte, avvisi di quota. */
@Composable
fun DiaryScreen(
    state: State, quotaEvents: List<Event>, history: List<Event>, nightReport: Event?, ttsMinChars: Int, onSpeak: (String) -> Unit,
    onAdd: () -> Unit, onRemove: (jobId: String) -> Unit,
) {
    val recap = state.recap
    val items = state.night.items
    val empty = recap.items.isEmpty() && state.night.queued == 0 && state.night.running == null && quotaEvents.isEmpty() && items.isNullOrEmpty() && history.isEmpty() && nightReport == null
    if (empty) {
        Column(Modifier.fillMaxSize().background(CmColors.bg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            EmptyDiaryScene(Modifier.fillMaxWidth().height(240.dp))
            Text(stringResource(R.string.diary_empty), color = CmColors.text2, style = MaterialTheme.typography.bodyLarge)
            if (items != null) AddButton(onAdd)
        }
        return
    }
    Column(Modifier.fillMaxSize().background(CmColors.bg)) {
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (recap.items.isNotEmpty()) {
            item { DiaryTitle(stringResource(R.string.diary_title, dateLabel(recap.date, androidx.compose.ui.platform.LocalConfiguration.current.locales[0]))) }
            items(recap.items) { it ->
                DiaryCard(it.project) {
                    Speakable(it.done, speak = it.done.length > ttsMinChars, onSpeak)
                    it.next?.let { n -> Text(stringResource(R.string.diary_next, n), color = CmColors.briefSecondary, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        item {
            DiaryCard(if (state.night.queued > 0) stringResource(R.string.night_queued, state.night.queued) else stringResource(R.string.night_empty)) {
                state.night.running?.let {
                    Text(stringResource(R.string.night_running, it), color = CmColors.busy, style = MaterialTheme.typography.bodyLarge)
                    LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), color = CmColors.busy, trackColor = CmColors.briefTrack)
                }
                // Contratto 1.17: senza `items` il relay è precedente e la coda non si tocca dall'app.
                if (items == null) Text(stringResource(R.string.night_update_pc), color = CmColors.briefSecondary, style = MaterialTheme.typography.bodyMedium)
                items?.forEach { job ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(job.name, fontWeight = FontWeight.SemiBold, color = CmColors.briefBig, style = MaterialTheme.typography.titleSmall)
                            Text(job.prompt, color = CmColors.briefSecondary, style = MaterialTheme.typography.bodyMedium)
                            if (job.started != null) Text(stringResource(R.string.night_started), color = CmColors.busy, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onRemove(job.id) }, enabled = job.started == null) { Text(stringResource(R.string.night_remove), color = CmColors.actionIcon) }
                    }
                }
            }
        }
        // Contratto 1.18: il resoconto dell'ultima notte e i diari dei giorni precedenti, dagli eventi.
        nightReport?.let { n ->
            item(key = "night-report") { DiaryCard(n.title) { Speakable(n.body, speak = true, onSpeak) } }
        }
        val older = history.filter { it.ref != recap.date }
        if (older.isNotEmpty()) {
            item(key = "older") { DiaryTitle(stringResource(R.string.diary_previous)) }
            items(older, key = { "h-" + it.key }) { e -> PastDay(e, ttsMinChars, onSpeak) }
        }
        if (quotaEvents.isNotEmpty()) {
            item { DiaryTitle(stringResource(R.string.quota_alerts)) }
            items(quotaEvents, key = { it.key }) { e ->
                DiaryCard(e.title, labelColor = CmColors.briefWarn) {
                    if (e.body.isNotBlank()) Text(e.body, color = CmColors.briefSecondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (items != null) AddButton(onAdd)
    }
}

/** Un giorno passato: il titolo, e il testo intero che si apre al tocco. */
@Composable
private fun PastDay(e: Event, ttsMinChars: Int, onSpeak: (String) -> Unit) {
    var open by androidx.compose.runtime.saveable.rememberSaveable(e.key) { androidx.compose.runtime.mutableStateOf(false) }
    DiaryCard(e.title, modifier = Modifier.clickable { open = !open }) {
        if (open) Speakable(e.body, speak = e.body.length > ttsMinChars, onSpeak)
    }
}

/** Titolo di sezione pesante, come nella Panoramica. */
@Composable
private fun DiaryTitle(text: String) =
    Text(text, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.padding(top = 8.dp, start = 4.dp))

/** La card del brief dell'orologio: superficie alta, angoli ampi, etichetta in testa. */
@Composable
private fun DiaryCard(
    label: String, modifier: Modifier = Modifier, labelColor: androidx.compose.ui.graphics.Color = CmColors.briefLabel,
    content: @Composable ColumnScope.() -> Unit,
) = Surface(color = CmColors.briefCard, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).then(modifier)) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = labelColor)
        content()
    }
}

/** Il solo bottone pieno della scheda Diario: «Aggiungi alla notte», solo con un relay 1.17. */
@Composable
private fun AddButton(onAdd: () -> Unit) = Button(
    onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
) { Text(stringResource(R.string.night_add)) }

/** Nella lingua del telefono, non in quella della JVM (negli snapshot usciva in inglese). */
private fun dateLabel(iso: String, locale: java.util.Locale): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale)) }.getOrDefault(iso)
