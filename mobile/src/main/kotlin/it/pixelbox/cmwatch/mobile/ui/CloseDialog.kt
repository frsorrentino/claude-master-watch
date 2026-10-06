package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterService
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** «sta lavorando», «ha finito il compito alle 10:48», «stessa conversazione di rino»: la sessione è aperta. */
@Composable
internal fun closeStateLine(s: Session): String {
    val c = MasterService.closeState(s)
    return when (c.kind) {
        MasterService.CloseState.Kind.WORKING -> stringResource(R.string.close_working, s.name)
        MasterService.CloseState.Kind.DUPLICATE -> stringResource(R.string.close_duplicate, s.name, c.of.orEmpty())
        MasterService.CloseState.Kind.FINISHED -> c.at?.let { stringResource(R.string.close_finished_at, s.name, clockHm(it)) } ?: stringResource(R.string.close_finished, s.name)
        MasterService.CloseState.Kind.STILL -> stringResource(R.string.close_still, s.name)
    }
}

internal fun clockHm(at: Long): String = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochSecond(at))

/**
 * La domanda prima di chiudere una sessione dal menu o con /exit scritto nel campo (Franz, 06/10 10:57, mockup approvato
 * 12:25): il nome nel titolo e nel tasto, lo stato, cosa succede. Dalla card della home la conferma sta nella card.
 */
@Composable
internal fun CloseDialog(s: Session, onDismiss: () -> Unit, onConfirm: () -> Unit) = AlertDialog(
    onDismissRequest = onDismiss, containerColor = CmColors.surface,
    title = { Text(stringResource(R.string.close_title, s.name)) },
    text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(closeStateLine(s), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            Text(stringResource(R.string.close_what), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
        }
    },
    confirmButton = {
        Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = CmColors.gone, contentColor = Color.White)) {
            Text(stringResource(R.string.cleanup_close, s.name))
        }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_keep), color = CmColors.actionIcon) } },
)
