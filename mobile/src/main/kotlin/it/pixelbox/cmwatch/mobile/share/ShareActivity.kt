package it.pixelbox.cmwatch.mobile.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.mobile.ui.ShareScreen
import it.pixelbox.cmwatch.transport.TransportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Il «Condividi» di Android verso una sessione (contratto 1.19): testo o immagine, via `claude-master report`. */
class ShareActivity : ComponentActivity() {
    private val app get() = application as PhoneApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
        val image: Uri? = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        setContent {
            CmPhoneTheme {
                val snap by app.repo.snapshot.collectAsStateWithLifecycle()
                var sending by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val state = snap.state ?: return@CmPhoneTheme
                ShareScreen(state, text, hasImage = image != null, sending = sending) { session, message ->
                    sending = true
                    scope.launch {
                        val note = runCatching {
                            val bytes = image?.let { withContext(Dispatchers.IO) { ImageShrink.jpeg(this@ShareActivity, it) } }
                            if (image != null && bytes == null) return@runCatching getString(R.string.share_unreadable)
                            app.repo.report(session, message, if (bytes != null) "image/jpeg" else null, bytes, state.share?.maxBytes ?: 0)
                            getString(R.string.share_sent, session)
                        }.getOrElse { e ->
                            when (e) {
                                is TransportException.TooLarge -> getString(R.string.share_too_large)
                                is TransportException.Network -> getString(R.string.share_offline)
                                else -> getString(R.string.share_failed)
                            }
                        }
                        Toast.makeText(this@ShareActivity, note, Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }
        }
    }
}
