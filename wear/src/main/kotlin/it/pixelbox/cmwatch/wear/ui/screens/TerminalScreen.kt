package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.TerminalText
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.MonoStyle

/** Terminale: 30 righe mono, una per riga, scorrimento orizzontale per non spezzarle. */
@Composable
fun TerminalScreen(name: String, text: String?, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val h = rememberScrollState()
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { Text(name, style = MonoStyle, color = CmColors.text2, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec)) }
            when {
                loading -> item { Text(stringResource(R.string.terminal_loading), color = CmColors.text2, modifier = Modifier.transformedHeight(this, spec)) }
                error != null -> item { Text(error, color = CmColors.gone, modifier = Modifier.transformedHeight(this, spec)) }
                text != null -> for (line in TerminalText.lines(text)) {
                    item { Text(line, style = MonoStyle, color = CmColors.text, maxLines = 1, modifier = Modifier.fillMaxWidth().horizontalScroll(h).transformedHeight(this, spec)) }
                }
            }
            item { WideButton(stringResource(R.string.terminal_refresh), onClick = onRefresh, primary = true, enabled = !loading, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) }
        }
    }
}
