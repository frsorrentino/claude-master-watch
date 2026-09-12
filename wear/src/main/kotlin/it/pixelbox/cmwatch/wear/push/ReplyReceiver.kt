package it.pixelbox.cmwatch.wear.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.haptics.Haptics
import kotlinx.coroutines.launch

/** Azioni della notifica: le prime due opzioni («1 · yes») e la risposta dettata («Rispondi»). */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val app = context.applicationContext as CmApp
        val session = intent.getStringExtra(SESSION) ?: return
        val n = intent.getIntExtra(OPTION, -1)
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(TEXT)?.toString()?.trim()
        val pending = goAsync()
        app.scope.launch {
            try {
                when {
                    n > 0 -> app.repo.answer(session, n)
                    !text.isNullOrEmpty() -> app.repo.prompt(session, text)
                }
                Haptics.play(context, Haptics.Kind.SENT)
                app.notifier.cancel(session)
            } finally { pending.finish() }
        }
    }

    companion object {
        const val ACTION = "it.pixelbox.cmwatch.REPLY"
        const val SESSION = "session"
        const val OPTION = "option"
        const val TEXT = "text"
    }
}
