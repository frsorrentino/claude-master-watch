package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
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
        val size = remember(name, width, style) {
            STEPS.filter { it.value <= style.fontSize.value }.firstOrNull { sp ->
                measurer.measure(name, style.copy(fontSize = sp), maxLines = 1, softWrap = false).size.width <= width
            }
        }
        if (size != null) Text(name, style = style.copy(fontSize = size), color = color, maxLines = 1, softWrap = false)
        else Text(TileTexts.breakable(name), style = style, color = color, maxLines = maxLines)
    }
}

private val STEPS = listOf(14.sp, 13.sp, 12.sp, 11.sp)
