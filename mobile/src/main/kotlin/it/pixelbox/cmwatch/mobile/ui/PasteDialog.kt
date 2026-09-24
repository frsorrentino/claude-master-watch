package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** «Incolla il codice»: il testo di `relay pair --text`, quando il QR non si legge. */
@Composable
fun PasteDialog(onPair: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CmColors.surfaceHigh,
        title = { Text(stringResource(R.string.paste_title)) },
        text = {
            OutlinedTextField(text, { text = it }, minLines = 4, modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
        },
        confirmButton = { Button({ onPair(text) }, enabled = text.isNotBlank()) { Text(stringResource(R.string.pair_button)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
