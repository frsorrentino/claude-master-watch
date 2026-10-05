package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * La conferma dell'invito a zero tocchi (Franz, 04/10 15:48): a quale PC, e se gli altri dispositivi restano (un'aggiunta)
 * o si scollegano (un accoppiamento nuovo). Senza il tocco su «Accoppia» non succede niente.
 */
@Composable
fun PairInviteDialog(host: String, add: Boolean, onPair: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CmColors.surfaceHigh,
        title = { Text(stringResource(R.string.invite_title, host)) },
        text = { Text(stringResource(if (add) R.string.invite_add else R.string.invite_new, host)) },
        confirmButton = {
            Button(onPair, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)) {
                Text(stringResource(R.string.pair_button))
            }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
