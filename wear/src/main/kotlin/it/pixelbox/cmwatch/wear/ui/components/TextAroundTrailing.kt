package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Text

/**
 * Testo che scorre attorno a un elemento in alto a destra (Franz, 16/09 13:39: il ▶ non deve limitare la colonna, il
 * testo prosegue a tutta larghezza sotto il tasto). Compose non ha il «float» del web: si misura il testo alla larghezza
 * ridotta, si prendono le righe che servono a coprire l'altezza del tasto, e il resto va sotto a tutta larghezza.
 * Il taglio cade sempre a fine riga, quindi le parole non si spezzano diversamente da come le mostrerebbe la colonna.
 */
@Composable
fun TextAroundTrailing(
    text: String,
    style: TextStyle,
    color: Color,
    trailingSize: Dp,
    modifier: Modifier = Modifier,
    gap: Dp = 6.dp,
    trailing: @Composable () -> Unit,
) = TextAroundTrailing(AnnotatedString(text), style, color, trailingSize, modifier, gap, trailing)

/** Come sopra, con il testo formattato (i pezzi in mono della risposta nel Terminale). */
@Composable
fun TextAroundTrailing(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    trailingSize: Dp,
    modifier: Modifier = Modifier,
    gap: Dp = 6.dp,
    trailing: @Composable () -> Unit,
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val stretta = (constraints.maxWidth - with(density) { (trailingSize + gap).roundToPx() }).coerceAtLeast(1)
        val taglio = remember(text, style, stretta, density) {
            val r = measurer.measure(text, style, constraints = Constraints(maxWidth = stretta))
            val alto = with(density) { trailingSize.toPx() }
            // Le righe accanto al tasto: tante quante servono perché il testo arrivi almeno al fondo del tasto.
            var n = 0
            while (n < r.lineCount && (n == 0 || r.getLineBottom(n - 1) < alto)) n++
            if (n >= r.lineCount) text.length else r.getLineEnd(n - 1)
        }
        Column {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Text(text.subSequence(0, taglio), style = style, color = color, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(gap))
                trailing()
            }
            // Il resto riparte dalla prima lettera: lo spazio dove la riga andava a capo non deve rientrare il testo.
            var inizio = taglio
            while (inizio < text.length && text[inizio].isWhitespace()) inizio++
            if (inizio < text.length) {
                Text(text.subSequence(inizio, text.length), style = style, color = color, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
