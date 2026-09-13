package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import androidx.compose.ui.res.stringResource
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.NameText
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.MonoStyle
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
) {
    val age = SessionsText.sub(s, now, stringResource(R.string.session_closed))
    val overflow = if (NameText.sharesPrefix(s.name, siblings)) TextOverflow.MiddleEllipsis else TextOverflow.Ellipsis
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(32.dp),
        contentPadding = PaddingValues(start = 10.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = if (ambient) CmColors.bg else CmColors.surface, contentColor = CmColors.text, secondaryContentColor = CmColors.text2),
        icon = { SessionBadge(s, size = 40.dp, ambient = ambient) },
        label = { Text(s.name, style = MonoStyle, maxLines = 1, overflow = overflow, softWrap = false, modifier = Modifier.cmMarquee(marquee)) },
        secondaryLabel = if (ambient || (age == null && s.question == null)) null else ({
            Column {
                age?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1) }
                s.question?.let { q -> Text(q.text, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            }
        }),
    )
}
