package it.pixelbox.cmwatch.wear.ui.screens

import it.pixelbox.cmwatch.rules.OutcomeText
import it.pixelbox.cmwatch.rules.SpeechText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import it.pixelbox.cmwatch.wear.ui.theme.morph

/** Esito: `short` grande, `full`, ▶ per leggerlo, «Leggi tutto» chiede al PC (terminale). */
@Composable
fun OutcomeScreen(snapshot: Snapshot, name: String, now: Long, ttsMinChars: Int, speaking: Boolean, onSpeak: (String) -> Unit, onReadAll: () -> Unit, onBack: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val s = snapshot.state?.sessions?.firstOrNull { it.name == name }
    val o = s?.outcome
    ScreenScaffold(
        scrollState = listState,
        edgeButton = { CmEdgeButton(stringResource(R.string.outcome_read_all), onClick = onReadAll) },
    ) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            if (s == null || o == null) {
                item { Text(stringResource(R.string.outcome_none), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
                item { WideButton(stringResource(R.string.sessions_title), onClick = onBack, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
                return@TransformingLazyColumn
            }
            // Niente riga del comando in corso: la schermata dice com'è finito il turno (Franz, 14/09 17:40).
            item { SessionHeader(s, now, true, modifier = Modifier.morph(this, spec), showTool = false) }
            // La frase intera dell'esito: grande se è breve, più piccola se è lunga (contratto 1.6, fino a 200 caratteri).
            // Il ▶ sta su una riga sua: accanto a un titolo lungo copriva il testo (Franz, 14/09 17:40).
            val titolo = OutcomeText.headline(o)
            item {
                Text(
                    titolo, color = CmColors.text, modifier = Modifier.fillMaxWidth().morph(this, spec),
                    style = if (OutcomeText.bigTitle(titolo)) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                )
            }
            if (SpeakRules.showButton(o.full, SpeakRules.Kind.OUTCOME, ttsMinChars)) {
                item { Box(Modifier.fillMaxWidth().morph(this, spec), contentAlignment = Alignment.Center) { SpeakButton(speaking, onToggle = { onSpeak(SpeechText.outcome(o)) }) } }
            }
            // Sotto, solo quello che il titolo non dice già (Franz, 14/09 14:09).
            OutcomeText.body(o)?.let { b -> item { Text(b, style = MaterialTheme.typography.bodyMedium, color = CmColors.text, modifier = Modifier.fillMaxWidth().morph(this, spec)) } }
        }
    }
}
