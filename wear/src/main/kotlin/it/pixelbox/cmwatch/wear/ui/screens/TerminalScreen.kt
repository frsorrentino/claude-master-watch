package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import it.pixelbox.cmwatch.rules.SpeechText
import it.pixelbox.cmwatch.rules.TerminalText
import it.pixelbox.cmwatch.wear.ui.components.SpeakButton
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.morph
import it.pixelbox.cmwatch.wear.ui.theme.MonoStyle
import it.pixelbox.cmwatch.wear.ui.theme.TerminalStyle

/** Terminale: 30 righe mono, una per riga, scorrimento orizzontale per non spezzarle. */
@Composable
fun TerminalScreen(
    name: String,
    text: String?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    speaking: Boolean = false,
    onSpeak: (String) -> Unit = {},
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = { CmEdgeButton(stringResource(R.string.terminal_refresh), onClick = onRefresh, enabled = !loading) },
    ) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            // Il ▶ del Terminale legge le ultime righe del terminale, non la risposta: quella è dell'Esito e della Scheda
            // (Franz, 14/09 15:20, «ok la tua proposta»).
            val parlato = text?.let { SpeechText.terminal(it) }?.takeIf { it.isNotBlank() }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().morph(this, spec)) {
                    Text(name, style = MonoStyle, color = CmColors.text2, modifier = Modifier.weight(1f))
                    if (parlato != null) { Spacer(Modifier.width(8.dp)); SpeakButton(speaking, onToggle = { onSpeak(parlato) }) }
                }
            }
            // Niente più «Risposta» in cima: era `outcome.full`, che la Scheda ora mostra intero (Franz, 15/09 17:02).
            when {
                loading -> item { Text(stringResource(R.string.terminal_loading), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
                error != null -> item { Text(error, color = CmColors.gone, modifier = Modifier.morph(this, spec)) }
                text != null -> for (row in TerminalText.rows(text)) {
                    // Niente scorrimento orizzontale: intercettava lo swipe di ritorno (Franz, 12/09 16:17). Le righe lunghe vanno a capo.
                    // Le teste di blocco (prompt, strumenti, elenchi, titoli) sono colorate e in grassetto: la gerarchia
                    // si vede senza spendere righe, e lo stacco è un filo di spazio, non una riga vuota (13/09 17:46).
                    when {
                        row.text.isEmpty() -> item { Spacer(Modifier.height(6.dp)) }
                        row.head -> item {
                            Text(
                                row.text, style = TerminalStyle.copy(fontWeight = FontWeight.Bold),
                                color = CmColors.busy, modifier = Modifier.fillMaxWidth().morph(this, spec),
                            )
                        }
                        else -> item { Text(row.text, style = TerminalStyle, color = CmColors.text, modifier = Modifier.fillMaxWidth().morph(this, spec)) }
                    }
                }
            }
        }
    }
}
