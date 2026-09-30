package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.TerminalText
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il terminale a schermo grande (design 29/09, schermata 3; restyling 30/09): dal vivo finché è aperto, come
 * sull'orologio. I colori dicono chi parla: tu, Claude, gli strumenti, l'output. «Aggiorna» è un'icona in testata e
 * l'indicatore ondulato gira durante la lettura; i blocchi nuovi entrano in dissolvenza.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TerminalScreen(name: String, text: String?, loading: Boolean, onRefresh: () -> Unit) {
    val off = animationsOff()
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, stringResource(R.string.refresh), tint = CmColors.actionIcon) }
        }
        Box(Modifier.fillMaxWidth().height(10.dp).padding(horizontal = 20.dp)) {
            if (loading) LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth(), color = CmColors.actionIcon, trackColor = CmColors.briefTrack)
        }
        if (text == null) {
            Text(stringResource(R.string.terminal_empty), color = CmColors.text2, modifier = Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp))
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(TerminalText.blocks(text), key = { i, b -> "$i-${b.text.hashCode()}" }) { _, b ->
                    val a = remember { Animatable(if (off) 1f else 0f) }
                    LaunchedEffect(Unit) { a.animateTo(1f, CmMotion.spec(off)) }
                    Text(
                        b.text, fontFamily = FontFamily.Monospace, modifier = Modifier.alpha(a.value),
                        color = speakerColor(b.kind),
                        fontWeight = if (b.heading || b.kind == TerminalText.Kind.USER) FontWeight.SemiBold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

/** Chi parla: tu in azzurro, Claude in bianco, gli strumenti in grigio chiaro, l'output nel grigio del dato vecchio. */
private fun speakerColor(k: TerminalText.Kind) = when (k) {
    TerminalText.Kind.USER -> CmColors.actionIcon
    TerminalText.Kind.CLAUDE -> CmColors.text
    TerminalText.Kind.TOOL -> CmColors.text2
    TerminalText.Kind.OUTPUT -> CmColors.stale
}
