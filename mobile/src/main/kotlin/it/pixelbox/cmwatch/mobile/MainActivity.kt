package it.pixelbox.cmwatch.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.pixelbox.cmwatch.mobile.pair.Phase
import it.pixelbox.cmwatch.mobile.ui.*
import it.pixelbox.cmwatch.pairing.PairingRecord
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app get() = application as PhoneApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CmPhoneTheme {
                val settings by app.prefs.flow.collectAsStateWithLifecycle(initialValue = null)
                val ui by app.pairing.ui.collectAsStateWithLifecycle()
                val scope = rememberCoroutineScope()
                var paste by remember { mutableStateOf(false) }
                // Dopo un riavvio per un altro progetto Firebase si riprende il QR salvato.
                LaunchedEffect(Unit) { app.pairing.resume() }
                LaunchedEffect(ui.phase) {
                    when (ui.phase) {
                        Phase.RESTART -> Restarter.restart(this@MainActivity)
                        Phase.DONE -> Buzz.done(this@MainActivity)
                        else -> Unit
                    }
                }
                fun scan() {
                    scope.launch {
                        when (val r = Scanner.scan(this@MainActivity)) {
                            is Scanner.Result.Read -> app.pairing.run(r.text)
                            Scanner.Result.Unavailable -> paste = true
                            Scanner.Result.Cancelled -> Unit
                        }
                    }
                }
                // Indietro da un errore: si torna alla schermata di prima, niente resta a metà.
                BackHandler(enabled = ui.phase == Phase.FAILED) { app.pairing.reset() }
                val s = settings ?: return@CmPhoneTheme
                when {
                    ui.phase != Phase.IDLE -> PairingScreen(
                        ui,
                        onRetry = { scope.launch { app.pairing.retry() } },
                        onRescan = ::scan,
                        onWithoutWatch = { scope.launch { app.pairing.retry(withoutWatch = true) } },
                        onInstallOnWatch = { scope.launch { app.pairing.installOnWatch() } },
                        onDone = { app.pairing.reset() },
                    )
                    s.paired -> {
                        val r = PairingRecord.fromJson(s.pairingJson)
                        PairedScreen(s.host.orEmpty(), app.phoneName, r?.watchName, r?.watchPending == true, onRepair = ::scan)
                    }
                    else -> NotPairedScreen(onPair = ::scan, onPaste = { paste = true })
                }
                if (paste) PasteDialog(onPair = { t -> paste = false; scope.launch { app.pairing.run(t) } }, onDismiss = { paste = false })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        app.scope.launch { app.pairing.completePending() }
    }
}
