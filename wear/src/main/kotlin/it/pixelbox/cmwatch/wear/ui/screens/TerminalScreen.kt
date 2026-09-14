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
import it.pixelbox.cmwatch.rules.TerminalText
import it.pixelbox.cmwatch.wear.ui.components.SpeakButton
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.edgeListPadding
import it.pixelbox.cmwatch.wear.ui.theme.roundListPadding
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
    answer: String? = null,
    speaking: Boolean = false,
    onSpeak: (String) -> Unit = {},
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(
        scrollState = listState,
        contentPadding = edgeListPadding(sides = 0.10f),
        edgeButton = { CmEdgeButton(stringResource(R.string.terminal_refresh), onClick = onRefresh, enabled = !loading) },
    ) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { Text(name, style = MonoStyle, color = CmColors.text2, modifier = Modifier.fillMaxWidth()) }
            // La risposta finale della sessione, che nella cattura del terminale spesso non c'è più: sta in cima, in
            // carattere proporzionale e più grande, così si riconosce dalla lavorazione (Franz, 13/09 18:11).
            answer?.takeIf { it.isNotBlank() }?.let { risposta ->
                item {
                    Text(
                        stringResource(R.string.terminal_answer), style = MaterialTheme.typography.labelMedium,
                        color = CmColors.briefLabel, modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            risposta, style = MaterialTheme.typography.bodyLarge, color = CmColors.text,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        SpeakButton(speaking, onToggle = { onSpeak(risposta) })
                    }
                }
                item { Spacer(Modifier.height(10.dp)) }
            }
            when {
                loading -> item { Text(stringResource(R.string.terminal_loading), color = CmColors.text2, modifier = Modifier) }
                error != null -> item { Text(error, color = CmColors.gone, modifier = Modifier) }
                text != null -> for (row in TerminalText.rows(text)) {
                    // Niente scorrimento orizzontale: intercettava lo swipe di ritorno (Franz, 12/09 16:17). Le righe lunghe vanno a capo.
                    // Le teste di blocco (prompt, strumenti, elenchi, titoli) sono colorate e in grassetto: la gerarchia
                    // si vede senza spendere righe, e lo stacco è un filo di spazio, non una riga vuota (13/09 17:46).
                    when {
                        row.text.isEmpty() -> item { Spacer(Modifier.height(6.dp)) }
                        row.head -> item {
                            Text(
                                row.text, style = TerminalStyle.copy(fontWeight = FontWeight.Bold),
                                color = CmColors.busy, modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        else -> item { Text(row.text, style = TerminalStyle, color = CmColors.text, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            }
        }
    }
}
