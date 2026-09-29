package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun DiaryScreen(state: State, quotaEvents: List<Event>, ttsMinChars: Int, onSpeak: (String) -> Unit) {
    val recap = state.recap
    val empty = recap.items.isEmpty() && state.night.queued == 0 && state.night.running == null && quotaEvents.isEmpty()
    if (empty) {
        Column(Modifier.fillMaxSize().background(CmColors.bg), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            EmptyDiaryScene(Modifier.fillMaxWidth().height(240.dp))
            Text(stringResource(R.string.diary_empty), color = CmColors.text2, style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().background(CmColors.bg), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (recap.items.isNotEmpty()) {
            item { Text(stringResource(R.string.diary_title, dateLabel(recap.date)), style = MaterialTheme.typography.titleLarge, color = CmColors.text) }
            items(recap.items) { it ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(it.project, fontWeight = FontWeight.SemiBold, color = CmColors.text, style = MaterialTheme.typography.titleSmall)
                    Speakable(it.done, speak = it.done.length > ttsMinChars, onSpeak)
                    it.next?.let { n -> Text(stringResource(R.string.diary_next, n), color = CmColors.text2, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (state.night.queued > 0) stringResource(R.string.night_queued, state.night.queued) else stringResource(R.string.night_empty),
                    color = CmColors.text, style = MaterialTheme.typography.titleSmall,
                )
                state.night.running?.let { Text(stringResource(R.string.night_running, it), color = CmColors.busy) }
            }
        }
        if (quotaEvents.isNotEmpty()) {
            item { Text(stringResource(R.string.quota_alerts), style = MaterialTheme.typography.titleMedium, color = CmColors.text) }
            items(quotaEvents, key = { it.key }) { e ->
                Column {
                    Text(e.title, color = CmColors.waiting, style = MaterialTheme.typography.titleSmall)
                    if (e.body.isNotBlank()) Text(e.body, color = CmColors.text2, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private fun dateLabel(iso: String): String =
    runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)) }.getOrDefault(iso)
