package it.pixelbox.cmwatch.wear.live

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.LiveTap
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.ui.Keyboard
import it.pixelbox.cmwatch.wear.ui.screens.LiveScreen
import it.pixelbox.cmwatch.wear.ui.theme.CmTheme
import kotlinx.coroutines.delay

/**
 * La scheda live a schermo intero, aperta dalla notifica fissa. «Parla» usa la dettatura del sistema (solo dentro la
 * live: specifica 06/10, §2); il testo va al telefono, che lo smista. Una dettatura vuota arriva lo stesso: la voce dice
 * «non ho capito».
 */
class LiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val live = (application as CmApp).live
        setContent {
            CmTheme {
                val card by live.card.collectAsStateWithLifecycle()
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(250) } }
                val talk = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
                    live.send(LiveTap(LiveTap.Action.SAY, text = Keyboard.result(r.data).orEmpty()))
                }
                LiveScreen(card, now, onTap = live::send, onTalk = { talk.launch(Keyboard.intent(getString(R.string.live_talk_label))) })
            }
        }
    }
}
