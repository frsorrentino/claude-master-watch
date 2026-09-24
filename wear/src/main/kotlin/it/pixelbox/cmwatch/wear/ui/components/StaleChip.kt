package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** «PC fermo da N min»: solo quando serve (design, sezione 5). */
@Composable
fun StaleChip(minutes: Int, modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxWidth().border(BorderStroke(1.dp, CmColors.line), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Due righe quando serve: su 384 px e con il carattere grande «PC offline for 12 min» perdeva «min» (notte del 24/09).
        Text(stringResource(R.string.pc_stale, minutes), color = CmColors.stale, style = MaterialTheme.typography.bodySmall, maxLines = 2, textAlign = TextAlign.Center)
    }
}
