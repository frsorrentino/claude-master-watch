package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.MonoStyle

/** Una riga a tutta larghezza: icona di stato, pallino dell'account, «nome · durata»; sotto, la domanda se c'è. */
@Composable
fun SessionRow(
    s: Session,
    now: Long,
    fresh: Boolean,
    onClick: () -> Unit,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
) {
    // Look da selettore app di Wear OS (Franz, 12/09 20:03): badge grande a sinistra, riga alta, angoli pieni, sfondo chiaro.
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        shape = RoundedCornerShape(32.dp),
        contentPadding = PaddingValues(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        transformation = transformation,
        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CmColors.surface, contentColor = CmColors.text, secondaryContentColor = CmColors.text2),
        icon = { SessionBadge(s, size = 40.dp) },
        label = {
            val row = SessionsText.row(s, now)
            Text(
                buildAnnotatedString {
                    withStyle(MonoStyle.toSpanStyle()) { append(s.name) }
                    append(row.removePrefix(s.name))
                },
                maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge,
            )
        },
        secondaryLabel = s.question?.let { q -> { Text(q.text, maxLines = 1, overflow = TextOverflow.Ellipsis, color = CmColors.text2) } },
    )
}
