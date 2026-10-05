package it.pixelbox.cmwatch.mobile.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import it.pixelbox.cmwatch.mobile.ui.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.ui.CmPhoneTheme
import it.pixelbox.cmwatch.rules.ContextActions
import it.pixelbox.cmwatch.rules.Sent
import it.pixelbox.cmwatch.ui.tokens.CmColors
import kotlinx.coroutines.launch

/**
 * Il foglio di scrittura del widget Master (spec 01/10): trasparente sopra la home, il campo già a fuoco con la tastiera
 * aperta. Invio = prompt alla master, o la risposta se la master sta chiedendo, con lo stesso percorso della scheda
 * (registrato nella chat). Poi si chiude.
 */
class WriteToMasterActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PhoneApp
        setContent {
            CmPhoneTheme {
                // Osservato, non letto una volta: a freddo lo stato arriva dopo la prima composizione (revisione finale 02/10).
                val snap by app.repo.snapshot.collectAsStateWithLifecycle()
                val master = ContextActions.master(snap.state)
                val asking = master?.question != null
                var text by rememberSaveable { mutableStateOf("") }
                val focus = remember { FocusRequester() }
                ModalBottomSheet(onDismissRequest = { finish() }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = CmColors.surface) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        master?.question?.let { q -> Text(q.text, style = MaterialTheme.typography.bodyLarge, color = CmColors.text) }
                        OutlinedTextField(
                            text, { text = it }, modifier = Modifier.fillMaxWidth().focusRequester(focus), minLines = 2, maxLines = 6,
                            placeholder = { Text(stringResource(if (asking) R.string.mw_reply else R.string.mw_write)) },
                        )
                        Button(
                            onClick = {
                                val m = master ?: return@Button
                                val t = text.trim().takeIf { it.isNotEmpty() } ?: return@Button
                                lifecycleScope.launch {
                                    val id = runCatching { if (asking) app.repo.answerText(m.name, t) else app.repo.prompt(m.name, t) }.getOrNull()
                                    val now = System.currentTimeMillis() / 1000
                                    if (id != null) app.chatLog.add(Sent(id, m.name, t, now, startedAt = if (asking) now else null))
                                    finish()
                                }
                            },
                            enabled = master != null && text.isNotBlank(), modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
                        ) { Text(stringResource(R.string.send)) }
                        // Dentro il foglio, che si compone in una finestra sua: fuori il fuoco non arrivava al campo.
                        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                    }
                }
            }
        }
    }
}
