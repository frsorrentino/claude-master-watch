package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.curvedComposable
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.TimeTextDefaults
import androidx.wear.compose.material3.timeTextCurvedText
import androidx.wear.compose.material3.timeTextSeparator
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * L'ora in alto con accanto, curvo e in ambra, quante sessioni aspettano una risposta (B10, Franz 15/09 23:40): si vede da
 * ogni schermata dell'app, senza tornare alla lista. Il fumetto al posto dell'emoji «❓», che usciva grossolano (16/09
 * 16:30). Niente quando nessuno aspetta o il dato è vecchio.
 */
@Composable
fun CmTimeText(waiting: Int) {
    val ambra = TimeTextDefaults.timeTextStyle(color = CmColors.waiting)
    TimeText { time ->
        timeTextCurvedText(time)
        if (waiting > 0) {
            timeTextSeparator()
            curvedComposable {
                Icon(painterResource(R.drawable.ic_state_question), contentDescription = null, tint = CmColors.waiting, modifier = Modifier.size(14.dp))
            }
            timeTextCurvedText(" $waiting", style = ambra)
        }
    }
}
