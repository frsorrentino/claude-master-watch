package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.AnswerText
import it.pixelbox.cmwatch.rules.TerminalText
import it.pixelbox.cmwatch.wear.ui.components.CmEdgeButton
import it.pixelbox.cmwatch.wear.ui.components.SpeakButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.Mono
import it.pixelbox.cmwatch.wear.ui.theme.MonoStyle
import it.pixelbox.cmwatch.wear.ui.theme.TerminalStyle
import it.pixelbox.cmwatch.wear.ui.theme.morph

/**
 * Risposta e Terminale (Franz, 15/09 17:19): in cima la risposta intera della sessione (contratto 1.4, `last`) a
 * paragrafi, in carattere normale e ascoltabile per blocchi; sotto le righe del terminale in mono, più piccole.
 * `answer` null = la sta ancora chiedendo; `current` = il paragrafo che la voce sta leggendo.
 */
@Composable
fun TerminalScreen(
    name: String,
    text: String?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    answer: List<AnswerText.Block>? = null,
    current: Int? = null,
    speaking: Boolean = false,
    onSpeakAll: () -> Unit = {},
    onBlock: (Int) -> Unit = {},
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    // La lista segue la voce: il paragrafo letto sale in vista (l'elemento 0 è la testata).
    LaunchedEffect(current) { current?.let { listState.animateScrollToItem(it + 1) } }
    ScreenScaffold(
        scrollState = listState,
        edgeButton = { CmEdgeButton(stringResource(R.string.terminal_refresh), onClick = onRefresh, enabled = !loading) },
    ) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().morph(this, spec)) {
                    Text(name, style = MonoStyle, color = CmColors.text2, modifier = Modifier.weight(1f))
                    if (!answer.isNullOrEmpty() || text != null) { Spacer(Modifier.width(8.dp)); SpeakButton(speaking, onToggle = onSpeakAll) }
                }
            }
            if (answer == null) {
                item { Text(stringResource(R.string.terminal_loading), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
                return@TransformingLazyColumn
            }
            // Toccare un paragrafo lo legge da lì in avanti; quello letto ha il fondo acceso.
            answer.forEachIndexed { i, b ->
                item { AnswerBlock(b, active = current == i, onClick = { onBlock(i) }, modifier = Modifier.morph(this, spec)) }
            }
            when {
                loading -> item { Text(stringResource(R.string.terminal_loading), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
                error != null -> item { Text(error, color = CmColors.gone, modifier = Modifier.morph(this, spec)) }
                text != null -> {
                    if (answer.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.terminal_lines), style = MaterialTheme.typography.labelMedium, color = CmColors.briefLabel,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).morph(this, spec),
                            )
                        }
                    }
                    for (row in TerminalText.rows(text)) {
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
}

/** Testo della risposta: carattere normale 16 sp, interlinea ampia; il mono resta a codice e percorsi. */
private val AnswerSize = 16.sp
private val AnswerLine = 23.sp

@Composable
private fun AnswerBlock(b: AnswerText.Block, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyLarge.copy(fontSize = AnswerSize, lineHeight = AnswerLine)
    Box(
        modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .background(if (active) CmColors.surfaceHigh else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 4.dp),
    ) {
        when (b.kind) {
            AnswerText.Kind.HEADING -> Text(
                b.text, color = CmColors.actionIcon,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            )
            AnswerText.Kind.CODE -> Text(
                b.text, style = TerminalStyle, color = CmColors.text,
                modifier = Modifier.fillMaxWidth().background(CmColors.surface, RoundedCornerShape(8.dp)).padding(8.dp),
            )
            AnswerText.Kind.BULLET -> Row {
                Text("•", style = style, color = CmColors.text2)
                Spacer(Modifier.width(8.dp))
                Text(inline(b.text), style = style, color = CmColors.text)
            }
            AnswerText.Kind.PARA -> Text(inline(b.text), style = style, color = CmColors.text)
        }
    }
}

/** I pezzi tra `backtick` in mono, più piccoli, su fondo scuro: percorsi e comandi si staccano dal testo. */
private fun inline(t: String): AnnotatedString = buildAnnotatedString {
    t.split('`').forEachIndexed { i, part ->
        if (i % 2 == 1) withStyle(SpanStyle(fontFamily = Mono, fontSize = 14.sp, background = CmColors.surfaceHigh)) { append(part) }
        else append(part)
    }
}
