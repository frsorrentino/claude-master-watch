package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.material3.placeholder
import androidx.wear.compose.material3.placeholderShimmer
import androidx.wear.compose.material3.rememberPlaceholderState
import it.pixelbox.cmwatch.wear.ui.components.FitName
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Risposta e Terminale (Franz, 15/09 17:19): in cima la risposta intera della sessione (contratto 1.4, `last`) a
 * paragrafi, in carattere normale e ascoltabile per blocchi; sotto il terminale come un copione (design 15/09 21:28):
 * le tue righe su un filo azzurro, la prosa di Claude in chiaro, strumenti e output in mono grigio, niente bolle.
 * `answer` null = la sta ancora chiedendo; `current` = il paragrafo che la voce sta leggendo; `capturedAt` = l'ora
 * dell'ultima cattura arrivata, detta nel divisore perché con l'aggiornamento dal vivo conta quanto è fresca.
 */
@Composable
fun TerminalScreen(
    name: String,
    text: String?,
    loading: Boolean,
    error: String?,
    answer: List<AnswerText.Block>? = null,
    current: Int? = null,
    speaking: Boolean = false,
    onSpeakAll: () -> Unit = {},
    onBlock: (Int) -> Unit = {},
    onWrite: () -> Unit = {},
    capturedAt: Long? = null,
) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val blocks = remember(text) { text?.let { TerminalText.blocks(it) }.orEmpty() }
    // La lista segue la voce: il paragrafo letto sale in vista (l'elemento 0 è la testata).
    LaunchedEffect(current) { current?.let { listState.animateScrollToItem(it + 1) } }
    // Dal vivo: chi è sceso in fondo vede arrivare le righe nuove; chi è risalito resta dov'è. Si decide solo mentre
    // l'utente scorre, così una cattura che allunga la lista non cambia la scelta da sola.
    var follow by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow {
            val li = listState.layoutInfo
            listState.isScrollInProgress to (li.visibleItems.lastOrNull()?.index == li.totalItemsCount - 1)
        }.collect { (moving, atBottom) -> if (moving) follow = atBottom }
    }
    var seen by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(text) {
        // La prima cattura non sposta la vista: in cima c'è la risposta, che si legge per prima.
        val total = 1 + (answer?.size ?: 0) + 1 + blocks.size
        if (text != null && seen != null && text != seen && follow && answer != null) listState.animateScrollToItem(total - 1)
        if (text != null) seen = text
    }
    ScreenScaffold(
        scrollState = listState,
        // «Scrivi» è l'azione della schermata, dopo aver letto (Franz, 15/09 18:15).
        edgeButton = { CmEdgeButton(stringResource(R.string.card_write), onClick = onWrite) },
    ) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item {
                // Niente più Aggiorna (Franz, 15/09 22:54): il Terminale si aggiorna da solo. Resta ▶ accanto al nome.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().morph(this, spec)) {
                    FitName(name, style = MonoStyle, color = CmColors.text2, modifier = Modifier.weight(1f))
                    if (!answer.isNullOrEmpty() || text != null) { Spacer(Modifier.width(4.dp)); SpeakButton(speaking, onToggle = onSpeakAll) }
                }
            }
            if (answer == null) {
                item { LoadingLines(stringResource(R.string.terminal_loading), Modifier.morph(this, spec)) }
                return@TransformingLazyColumn
            }
            // Toccare un paragrafo lo legge da lì in avanti; quello letto ha il fondo acceso.
            answer.forEachIndexed { i, b ->
                item { AnswerBlock(b, active = current == i, onClick = { onBlock(i) }, modifier = Modifier.morph(this, spec)) }
            }
            when {
                loading -> item { LoadingLines(stringResource(R.string.terminal_loading), Modifier.morph(this, spec)) }
                error != null -> item { Text(error, color = CmColors.gone, modifier = Modifier.morph(this, spec)) }
                text != null -> {
                    item { TerminalDivider(capturedAt, Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp).morph(this, spec)) }
                    // Niente scorrimento orizzontale: intercettava lo swipe di ritorno (Franz, 12/09 16:17). Le righe lunghe
                    // vanno a capo. Lo stacco fra chi parla è uno spazio di 8 dp, mai una riga vuota.
                    // Chiave presa dal contenuto (A3, 15/09 23:40): a ogni cattura dal vivo i blocchi già visti restano fermi
                    // e solo quelli nuovi entrano, con dissolvenza e allungamento, invece di ridisegnare tutto.
                    val volte = HashMap<String, Int>()
                    blocks.forEachIndexed { i, b ->
                        val top = if (i > 0 && (b.kind == TerminalText.Kind.USER || b.kind == TerminalText.Kind.CLAUDE)) 8.dp else 0.dp
                        val base = "${b.kind}:${b.text.hashCode()}"
                        val n = volte.merge(base, 1, Int::plus) ?: 1
                        item(key = "$base#$n") { TerminalBlock(b, Modifier.fillMaxWidth().padding(top = top).morph(this, spec).animateItem()) }
                    }
                }
            }
        }
    }
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")

/**
 * Mentre il PC risponde (B8, 15/09 23:40): tre righe con la sagoma del testo che luccicano, al posto di «Chiedo al PC».
 * La frase resta, detta a TalkBack.
 */
@Composable
private fun LoadingLines(label: String, modifier: Modifier = Modifier) {
    val state = rememberPlaceholderState(isVisible = true)
    Column(modifier.fillMaxWidth().padding(vertical = 6.dp).semantics { contentDescription = label }) {
        listOf(1f, 0.85f, 0.6f).forEach { w ->
            Box(Modifier.fillMaxWidth(w).height(14.dp).placeholderShimmer(state).placeholder(state))
            Spacer(Modifier.height(10.dp))
        }
    }
}

/** Filo · «Terminale · 21:18» · filo: separa la risposta dalle righe e dice quanto è fresca la cattura. */
@Composable
private fun TerminalDivider(capturedAt: Long?, modifier: Modifier = Modifier) {
    val label = capturedAt?.let { stringResource(R.string.terminal_divider, HHMM.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))) }
        ?: stringResource(R.string.card_terminal)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(Modifier.weight(1f).height(1.dp).background(CmColors.briefTrack))
        // L'ora nuova entra in dissolvenza a ogni cattura dal vivo (A3, 15/09 23:40): si nota che è cambiata, senza scatti.
        AnimatedContent(targetState = label, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "ora") {
            Text(it, style = MaterialTheme.typography.labelMedium, color = CmColors.briefLabel, modifier = Modifier.padding(horizontal = 8.dp))
        }
        Box(Modifier.weight(1f).height(1.dp).background(CmColors.briefTrack))
    }
}

/** Righe del terminale: il carattere dice chi parla. Tu e Claude in sans come un testo da leggere, il resto in mono. */
private val VoiceSize = 15.sp
private val VoiceLine = 21.sp

@Composable
private fun TerminalBlock(b: TerminalText.Block, modifier: Modifier = Modifier) {
    val voice = MaterialTheme.typography.bodyMedium.copy(fontSize = VoiceSize, lineHeight = VoiceLine)
    when (b.kind) {
        // Il filo è dentro l'item e alto quanto il blocco: resta continuo anche mentre la lista si deforma.
        TerminalText.Kind.USER -> Row(modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(CmColors.accent))
            Spacer(Modifier.width(8.dp))
            Text(b.text, style = voice, color = CmColors.actionIcon)
        }
        TerminalText.Kind.CLAUDE -> Text(
            b.text, style = if (b.heading) voice.copy(fontWeight = FontWeight.Bold) else voice, color = CmColors.text, modifier = modifier,
        )
        TerminalText.Kind.TOOL -> Text(b.text, style = TerminalStyle.copy(fontWeight = FontWeight.Bold), color = CmColors.briefSecondary, modifier = modifier)
        TerminalText.Kind.OUTPUT -> Text(b.text, style = TerminalStyle, color = CmColors.briefSecondary, modifier = modifier)
    }
}

/** Testo della risposta: carattere normale 16 sp, interlinea ampia; il mono resta a codice e percorsi. */
private val AnswerSize = 16.sp
private val AnswerLine = 23.sp

@Composable
private fun AnswerBlock(b: AnswerText.Block, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val style = MaterialTheme.typography.bodyLarge.copy(fontSize = AnswerSize, lineHeight = AnswerLine)
    // Il paragrafo letto si accende piano invece di scattare (proposta A4, 15/09 23:40), con il tempo del motion scheme.
    val fondo by animateColorAsState(if (active) CmColors.surfaceHigh else Color.Transparent, MaterialTheme.motionScheme.defaultEffectsSpec(), label = "paragrafo")
    Box(
        modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
            .background(fondo)
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
