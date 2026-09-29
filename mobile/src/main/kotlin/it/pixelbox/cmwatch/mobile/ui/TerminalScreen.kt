package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.TerminalText
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Il terminale a schermo grande (design 29/09, schermata 3): letto a richiesta, i blocchi nuovi entrano in dissolvenza. */
@Composable
fun TerminalScreen(name: String, text: String?, loading: Boolean, onRefresh: () -> Unit) {
    val off = animationsOff()
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        Text(name, style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.padding(20.dp))
        if (text == null) {
            Text(stringResource(R.string.terminal_empty), color = CmColors.text2, modifier = Modifier.weight(1f).padding(horizontal = 20.dp))
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(TerminalText.blocks(text), key = { i, b -> "$i-${b.text.hashCode()}" }) { _, b ->
                    val a = remember { Animatable(if (off) 1f else 0f) }
                    LaunchedEffect(Unit) { a.animateTo(1f, CmMotion.spec(off)) }
                    Text(
                        b.text, fontFamily = FontFamily.Monospace, modifier = Modifier.alpha(a.value),
                        color = if (b.kind == TerminalText.Kind.CLAUDE) CmColors.text else CmColors.text2,
                        fontWeight = if (b.heading) FontWeight.SemiBold else FontWeight.Normal, style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Button(
            onClick = onRefresh, enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
        ) { Text(stringResource(R.string.refresh)) }
    }
}
