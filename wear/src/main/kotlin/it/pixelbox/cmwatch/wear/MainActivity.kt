package it.pixelbox.cmwatch.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.TimeText
import it.pixelbox.cmwatch.wear.ui.screens.SessionsScreen
import it.pixelbox.cmwatch.wear.ui.theme.CmTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as CmApp
        setContent {
            CmTheme {
                AppScaffold(timeText = { TimeText() }) {
                    val snapshot by app.repo.snapshot.collectAsStateWithLifecycle()
                    val now by produceState(System.currentTimeMillis() / 1000) {
                        while (true) { delay(30_000); value = System.currentTimeMillis() / 1000 }
                    }
                    SessionsScreen(snapshot, now, onOpen = {}, onSettings = {})
                }
            }
        }
    }
}
