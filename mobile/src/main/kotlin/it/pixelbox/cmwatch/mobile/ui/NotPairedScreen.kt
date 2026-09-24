package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Primo avvio: due righe e un solo bottone pieno (design 24/09, schermata 1). */
@Composable
fun NotPairedScreen(onPair: () -> Unit, onPaste: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding().padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.weight(1f))
        Text(stringResource(R.string.not_paired_title), style = MaterialTheme.typography.headlineMedium, color = CmColors.text)
        Text(stringResource(R.string.not_paired_body), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2)
        Spacer(Modifier.weight(1f))
        Button(onClick = onPair, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.pair_button)) }
        TextButton(onClick = onPaste, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.paste_code), color = CmColors.actionIcon) }
    }
}
