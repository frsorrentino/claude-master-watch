package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.rules.TileTexts

/**
 * Il nome della sessione su una riga (proposta A2, Franz 15/09 23:40): prima si rimpicciolisce a scalini, dalla misura
 * dello stile fino a 11 sp, e solo se non basta va a capo come prima, dopo i trattini. Così «ledger-api» accanto al ▶
 * non diventa più «ledger- / api». Il `Text` di Wear M3 non ha `autoSize`: la misura la fa un `TextMeasurer`.
 */
@Composable
fun FitName(name: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, maxLines: Int = 3) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        val steps = STEPS.filter { it.value <= style.fontSize.value }
        fun fits(text: String, sp: TextUnit) = measurer.measure(text, style.copy(fontSize = sp), maxLines = 1, softWrap = false).size.width <= width
        val size = remember(name, width, style) { steps.firstOrNull { sp -> fits(name, sp) } }
        // Quando va a capo, la misura è la più grande in cui ogni pezzo fra i trattini sta su una riga: a 14 sp su
        // 384 px «ledger-api» si spezzava a metà parola («ledge / r-api», notte del 24/09), non dopo il trattino.
        val wrapSize = remember(name, width, style) {
            val pieces = name.split(PIECE).filter { it.isNotEmpty() }
            steps.firstOrNull { sp -> pieces.all { fits(it, sp) } } ?: style.fontSize
        }
        if (size != null) Text(name, style = style.copy(fontSize = size), color = color, maxLines = 1, softWrap = false)
        else Text(TileTexts.breakable(name), style = style.copy(fontSize = wrapSize), color = color, maxLines = maxLines)
    }
}

private val STEPS = listOf(14.sp, 13.sp, 12.sp, 11.sp)

/** Gli stessi punti di a-capo di `TileTexts.breakable`: dopo trattino, trattino basso e barra. */
private val PIECE = Regex("(?<=[-_/])")
