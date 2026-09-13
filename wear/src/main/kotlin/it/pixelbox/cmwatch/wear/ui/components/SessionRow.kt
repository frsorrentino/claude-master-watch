package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
import it.pixelbox.cmwatch.rules.ToolText
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
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
) {
    val age = SessionsText.sub(s, now, stringResource(R.string.session_closed))
    val overflow = if (NameText.sharesPrefix(s.name, siblings)) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis
    val cell = if (ambient) SessionsText.Cell(null, null) else SessionsText.cell(
        s, now, stringResource(R.string.tile_turn_running), stringResource(R.string.state_idle), tools,
    )
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        // Misure prese sulle celle di notifica di Wear OS (13/09 21:32): raggio 20 dp, badge 16 dp in linea con il
        // nome, non un bollo da 36 a tutta altezza, e quattro righe di testo sotto.
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ambient) CmColors.bg else CmColors.surface,
            contentColor = CmColors.text,
        ),
        contentPadding = PaddingValues(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp),
        transformation = transformation,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SessionBadge(s, size = 16.dp, ambient = ambient)
            Spacer(Modifier.width(8.dp))
            Text(
                s.name, style = SessionNameStyle, color = CmColors.text2, maxLines = 1, overflow = overflow,
                softWrap = false, modifier = Modifier.weight(1f).cmMarquee(marquee),
            )
            age?.let {
                Spacer(Modifier.width(6.dp))
                Text(
                    it, style = MaterialTheme.typography.bodySmall,
                    color = if (s.question != null) CmColors.waiting else CmColors.text2, maxLines = 1,
                )
            }
        }
        cell.title?.takeIf { it.isNotBlank() }?.let {
            Text(
                it, style = MaterialTheme.typography.bodyLarge, color = CmColors.text,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
            )
        }
        cell.detail?.takeIf { it.isNotBlank() }?.let {
            Text(
                it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
