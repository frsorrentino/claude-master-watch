package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Un file appena salvato dalla chat: il nome, dove sta (`uri`), il tipo, la copia in cache, e se è finito in Download. */
data class SavedFile(val name: String, val uri: String, val mime: String?, val path: String, val inDownloads: Boolean)

/**
 * La conferma dopo «Scarica» (Franz, 10/10 06:50: «servirebbe conferma download e opzioni apri cartella o apri file»): il
 * nome, dove è finito, «Apri» pieno, «Apri la cartella» (Download, che il sistema sa aprire) e «Salva altrove», che apre il
 * foglio di sistema per scegliere cartella e nome. Senza `onFolder` o `onElsewhere` il tasto non c'è.
 */
@Composable
fun FileSavedContent(f: SavedFile, onOpen: () -> Unit, onFolder: (() -> Unit)?, onElsewhere: (() -> Unit)?) {
    Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.CheckCircle, null, tint = CmColors.idle, modifier = Modifier.padding(top = 2.dp).size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.file_saved_to, f.name), style = MaterialTheme.typography.titleMedium, color = CmColors.text)
                Text(
                    stringResource(if (f.inDownloads) R.string.file_saved_downloads else R.string.file_saved_picked),
                    style = MaterialTheme.typography.bodyMedium, color = CmColors.stale,
                )
            }
        }
        Button(
            onClick = onOpen, colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(stringResource(R.string.file_open)) }
        if (onFolder != null || onElsewhere != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            onFolder?.let { go -> OutlinedButton(onClick = go, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.file_open_folder), color = CmColors.actionIcon) } }
            onElsewhere?.let { go -> OutlinedButton(onClick = go, modifier = Modifier.weight(1f).height(48.dp)) { Text(stringResource(R.string.file_save_elsewhere), color = CmColors.actionIcon) } }
        }
    }
}
