package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.wear.compose.material3.Icon
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import androidx.compose.ui.res.stringResource
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.NameText
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.TileTexts
import it.pixelbox.cmwatch.rules.ToolText
import it.pixelbox.cmwatch.ui.tokens.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.SessionNameStyle
import it.pixelbox.cmwatch.wear.ui.theme.cmMarquee

/**
 * Riga della lista come nel selettore app di Wear OS: badge grande a sinistra, nome su una o due righe intere,
 * durata e domanda su righe proprie in grigio. Il nome non viene mai tagliato in coda quando un'altra sessione
 * visibile condivide il prefisso (Franz, 13/09): in quel caso l'ellissi sta in mezzo e la coda distintiva resta.
 */
@Composable
fun SessionRow(
    s: Session,
    now: Long,
    fresh: Boolean,
    onClick: () -> Unit,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    siblings: List<String> = emptyList(),
    ambient: Boolean = false,
    marquee: Boolean = false,
    tools: ToolText.Labels? = null,
    onReopen: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    reopen: it.pixelbox.cmwatch.rules.ReopenText.Status? = null,
) {
    val age = SessionsText.sub(s, now, stringResource(R.string.session_closed))
    val overflow = if (NameText.sharesPrefix(s.name, siblings)) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis
    val cell = if (ambient) SessionsText.Cell(null, null) else SessionsText.cell(
        s, now, stringResource(R.string.tile_turn_running), stringResource(R.string.state_idle), tools,
    )
    Card(
        onClick = onClick,
        // Pressione lunga = segui / smetti (Franz, 15/09 18:59), in ambient no.
        onLongClick = if (ambient) null else onLongClick,
        modifier = modifier.fillMaxWidth(),
        // Misure prese sulle celle di notifica di Wear OS (13/09 21:32): raggio 20 dp, badge 16 dp in linea con il
        // nome, non un bollo da 36 a tutta altezza, e quattro righe di testo sotto.
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ambient) CmColors.bg else CmColors.surface,
            contentColor = CmColors.text,
        ),
        // La seguita è «accesa»: bordo giallino e campanella accanto al nome (Franz, 14/09 16:27, «entrambe»).
        border = if (s.followed) BorderStroke(1.5.dp, CmColors.followed) else null,
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        transformation = transformation,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SessionBadge(s, size = 16.dp, ambient = ambient)
            Spacer(Modifier.width(8.dp))
            // PROPOSTA (notte del 24/09): su 384 px «ledger-api» diventava «ledger-a…»; prima si rimpicciolisce fino a
            // 11 sp, come il nome nella testata (FitName), e solo poi scorre o si tronca come oggi.
            BoxWithConstraints(Modifier.weight(1f)) {
                val measurer = rememberTextMeasurer()
                val width = constraints.maxWidth
                val size = remember(s.name, width) {
                    listOf(13.sp, 12.sp, 11.sp).firstOrNull { sp ->
                        measurer.measure(s.name, SessionNameStyle.copy(fontSize = sp), maxLines = 1, softWrap = false).size.width <= width
                    } ?: 11.sp
                }
                Text(
                    s.name, style = SessionNameStyle.copy(fontSize = size), color = CmColors.text2, maxLines = 1, overflow = overflow,
                    softWrap = false, modifier = Modifier.cmMarquee(marquee),
                )
            }
            if (s.followed) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Rounded.Notifications, contentDescription = stringResource(R.string.session_followed),
                    tint = CmColors.followed, modifier = Modifier.size(14.dp),
                )
            }
            age?.let {
                Spacer(Modifier.width(6.dp))
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = if (s.question != null) CmColors.waiting else CmColors.text2, maxLines = 1,
                )
            }
        }
        // Quattro righe in tutto: se il titolo sta su una riga, al testo sotto ne restano tre (Franz, 14/09 08:10).
        var righeTitolo by remember(cell.title) { mutableIntStateOf(1) }
        cell.titleText?.takeIf { it.isNotBlank() }?.let {
            // Un pensiero intero, mai «…» (Franz, 14/09 18:16); quattro righe se sotto non c'è il prossimo passo, due se
            // c'è (Franz, 15/09 08:39). Righe e taglio li decide `SessionsText.Cell`.
            Text(
                TileTexts.breakable(it), style = MaterialTheme.typography.bodyLarge, color = CmColors.text,
                maxLines = cell.titleLines, modifier = Modifier.fillMaxWidth(),
                onTextLayout = { righeTitolo = it.lineCount },
            )
        }
        cell.detail?.takeIf { it.isNotBlank() }?.let {
            Text(
                // Misurato sulla cattura del 14/09 20:19: una riga ne tiene circa 22, quindi tre righe 60 e due 40.
                TileTexts.fitTile(it, max = if (righeTitolo <= 1) 60 else 40), style = MaterialTheme.typography.bodySmall,
                color = CmColors.text2, maxLines = if (righeTitolo <= 1) 3 else 2, modifier = Modifier.fillMaxWidth(),
            )
        }
        // Una chiusa si riprende dalla lista, senza aprire la scheda (contratto 1.9, `reopen`). In ambient no.
        if (onReopen != null && !ambient) {
            Spacer(Modifier.height(8.dp))
            // Subito dopo il tocco «Avvio in corso», spento: il rilancio sul PC dura 30-60 s (Franz, 15/09 19:14).
            val avvio = reopen is it.pixelbox.cmwatch.rules.ReopenText.Status.Starting
            androidx.compose.foundation.layout.Box(
                Modifier.clip(RoundedCornerShape(percent = 50)).background(if (avvio) CmColors.surfaceHigh else CmColors.primary)
                    .clickable(enabled = !avvio, onClick = onReopen).padding(horizontal = 14.dp, vertical = 6.dp),
            ) { Text(stringResource(if (avvio) R.string.reopen_starting else R.string.notif_resume), style = MaterialTheme.typography.labelMedium, color = if (avvio) CmColors.text else CmColors.onPrimary) }
            (reopen as? it.pixelbox.cmwatch.rules.ReopenText.Status.Failed)?.let { f ->
                Text(
                    stringResource(R.string.reopen_failed, f.text ?: stringResource(R.string.question_not_delivered)),
                    style = MaterialTheme.typography.bodySmall, color = CmColors.goneDim, modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}
