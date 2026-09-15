package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/**
 * L'ora in alto con accanto, curvo e in ambra, quante sessioni aspettano una risposta (B10, Franz 15/09 23:40): «❓ 1»
 * si vede da ogni schermata dell'app, senza tornare alla lista. Niente quando nessuno aspetta o il dato è vecchio.
 */
@Composable
fun CmTimeText(waiting: Int) {
    val ambra = TimeTextDefaults.timeTextStyle(color = CmColors.waiting)
    TimeText { time ->
        timeTextCurvedText(time)
        if (waiting > 0) {
            timeTextSeparator()
            timeTextCurvedText("❓ $waiting", style = ambra)
        }
    }
}
