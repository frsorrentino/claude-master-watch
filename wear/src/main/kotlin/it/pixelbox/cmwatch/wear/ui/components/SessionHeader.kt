package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.ui.res.stringResource
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.ToolText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.MonoStyle

/** Testata di Scheda e Domanda: icona di stato, pallino dell'account, «nome · durata» su una riga; il tool su una riga propria. */
@Composable
fun SessionHeader(
    s: Session, now: Long, fresh: Boolean, modifier: Modifier = Modifier, showTool: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 16 dp come nella lista (misura sulle celle di notifica di Wear OS): da 32 il tondo lo tagliava (Franz, 14/09 17:20).
            SessionBadge(s, size = 16.dp)
            Spacer(Modifier.width(8.dp))
            val row = SessionsText.row(s, now)
            val tail = row.removePrefix(s.name).removePrefix(" · ")
            Column(Modifier.weight(1f)) {
                // Il nome va a capo invece di scorrere (S07): lo scorrimento fermo a metà lasciava una «h» isolata.
                // …e va a capo dopo un trattino, non a metà parola («claude-master-w / atch», Franz 15/09 10:56).
                // Tre righe: «francescosorrentino-com-2» accanto al ▶ ne prendeva due e perdeva la coda (Franz, 15/09 19:01).
                // Prima rimpicciolisce fino a 11 sp per stare su una riga, poi va a capo (proposta A2, 15/09 23:40).
                FitName(s.name, style = MonoStyle, color = CmColors.text, maxLines = 3)
                // La campanella gialla accanto all'età quando la sessione è seguita: l'interruttore non c'è più (15/09 19:04).
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (tail.isNotEmpty()) Text(tail, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1)
                    if (s.followed) {
                        Spacer(Modifier.width(4.dp))
                        androidx.wear.compose.material3.Icon(
                            androidx.compose.material.icons.Icons.Rounded.Notifications, contentDescription = stringResource(R.string.session_followed),
                            tint = CmColors.followed, modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
            // Il ▶ della Domanda accanto al nome: sotto rubava la colonna destra al testo (Franz, 15/09 16:13).
            trailing?.let { Spacer(Modifier.width(8.dp)); it() }
        }
        // Cosa sta facendo, detto per esteso: la description del comando (contratto 1.5) o la frase dallo strumento.
        val tool = ToolText.describe(s.toolNote, s.tool, toolLabels())
        // Due righe e un pensiero intero, mai «…» (Franz, 14/09 17:40): la stessa regola della card della tile.
        if (showTool && s.state == SessionState.BUSY && tool != null) {
            Text(TileTexts.fitTile(tool, max = 56), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 2)
        }
    }
}

@Composable
private fun toolLabels() = ToolText.Labels(
    run = stringResource(R.string.tool_run), read = stringResource(R.string.tool_read),
    edit = stringResource(R.string.tool_edit), write = stringResource(R.string.tool_write),
    search = stringResource(R.string.tool_search), web = stringResource(R.string.tool_web),
    message = stringResource(R.string.tool_message), delegate = stringResource(R.string.tool_delegate),
    plan = stringResource(R.string.tool_plan), other = stringResource(R.string.tool_other),
)
