package it.pixelbox.cmwatch.wear.ui.screens

import it.pixelbox.cmwatch.rules.OutcomeText
import it.pixelbox.cmwatch.rules.SpeechText
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.rules.SpeakRules
import it.pixelbox.cmwatch.wear.ui.components.SessionHeader
import it.pixelbox.cmwatch.wear.ui.components.SpeakButton
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.edgeListPadding
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding

/** Esito: `short` grande, `full`, ▶ per leggerlo, «Leggi tutto» chiede al PC (terminale). */
@Composable
fun OutcomeScreen(snapshot: Snapshot, name: String, now: Long, ttsMinChars: Int, speaking: Boolean, onSpeak: (String) -> Unit, onReadAll: () -> Unit, onBack: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val s = snapshot.state?.sessions?.firstOrNull { it.name == name }
    val o = s?.outcome
    ScreenScaffold(
        scrollState = listState,
        contentPadding = edgeListPadding(sides = 0.07f),
        edgeButton = { CmEdgeButton(stringResource(R.string.outcome_read_all), onClick = onReadAll) },
    ) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            if (s == null || o == null) {
                item { Text(stringResource(R.string.outcome_none), color = CmColors.text2, modifier = Modifier) }
                item { WideButton(stringResource(R.string.sessions_title), onClick = onBack, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                return@TransformingLazyColumn
            }
            item { SessionHeader(s, now, true, modifier = Modifier) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    // La frase intera dell'esito, in un carattere che la fa stare (Franz, 14/09 13:20).
                    Text(OutcomeText.headline(o), style = MaterialTheme.typography.titleMedium, color = CmColors.text, modifier = Modifier.weight(1f))
                    if (SpeakRules.showButton(o.full, SpeakRules.Kind.OUTCOME, ttsMinChars)) {
                        Spacer(Modifier.width(8.dp)); SpeakButton(speaking, onToggle = { onSpeak(SpeechText.outcome(o)) })
                    }
                }
            }
            // Sotto, solo quello che il titolo non dice già (Franz, 14/09 14:09).
            OutcomeText.body(o)?.let { b -> item { Text(b, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.fillMaxWidth()) } }
        }
    }
}
