package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.ui.tokens.CmColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * Il motivo di un comando rifiutato dal PC, intero (dal vivo 01/10 23:17: il toast tagliava «reopen failed: claude-master
 * launch: 6 se…» e non si capiva che era il tetto delle sessioni al lavoro). In basso, per 10 s o finché non lo tocchi.
 */
@Composable
fun RefusalBanner(results: Flow<CmdResult>, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var shown by remember { mutableStateOf(false) }
    var at by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { results.collect { r -> if (!r.ok && r.text.isNotBlank()) { text = r.text; shown = true; at = System.nanoTime() } } }
    LaunchedEffect(at) { if (shown) { delay(10_000); shown = false } }
    val off = animationsOff()
    AnimatedVisibility(
        shown, modifier,
        enter = if (off) EnterTransition.None else fadeIn() + slideInVertically { it / 2 },
        exit = if (off) ExitTransition.None else fadeOut() + slideOutVertically { it / 2 },
    ) {
        Surface(
            onClick = { shown = false }, color = CmColors.surfaceHigh, shape = MaterialTheme.shapes.medium, shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = CmColors.gone)
                Text(text, style = MaterialTheme.typography.bodyMedium, color = CmColors.text)
            }
        }
    }
}
