package it.pixelbox.cmwatch.wear.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import it.pixelbox.cmwatch.contract.CmdOp
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.complication.CmComplicationService
import it.pixelbox.cmwatch.wear.haptics.Haptics
import it.pixelbox.cmwatch.wear.tile.CmTileService
import kotlinx.coroutines.launch

/** Azioni delle notifiche, senza aprire l'app: opzione diretta, risposta (chip o dettata), Riprova, Riprendi, «visto» al dismiss. */
class ReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as CmApp
        val session = intent.getStringExtra(SESSION) ?: return
        val pending = goAsync()
        app.scope.launch {
            try {
                when (intent.action) {
                    ACTION_OPTION -> {
                        val n = intent.getIntExtra(OPTION, -1); if (n < 1) return@launch
                        val id = app.repo.answer(session, n)
                        app.notifier.sent(session, n, intent.getStringExtra(OPTION_LABEL)?.substringAfter(' '), id)
                        Haptics.play(context, Haptics.Kind.SENT)
                    }
                    ACTION_REPLY -> {
                        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(TEXT)?.toString()?.trim().orEmpty()
                        if (text.isEmpty()) return@launch
                        // Un chip «2 no» (o «2») è un'opzione; tutto il resto è testo libero → prompt.
                        val n = Regex("^(\\d{1,2})(\\s|$)").find(text)?.groupValues?.get(1)?.toInt()
                        val id = if (n != null) app.repo.answer(session, n) else app.repo.prompt(session, text)
                        app.notifier.sent(session, n, if (n != null) text.substringAfter(' ', "").ifEmpty { null } else null, id)
                        Haptics.play(context, Haptics.Kind.SENT)
                    }
                    ACTION_RETRY -> intent.getStringExtra(CMD_ID)?.let { app.repo.retry(it); app.notifier.sent(session, null, null, it) }
                    ACTION_RESUME -> { app.repo.command(CmdOp.RESUME, session, null); app.notifier.cancel(session) }
                    ACTION_SEEN -> {
                        val qid = intent.getStringExtra(QUESTION_ID) ?: return@launch
                        app.prefs.update { it.copy(seenQuestions = it.seenQuestions + qid) }
                        runCatching { CmTileService.requestUpdate(app) }; runCatching { CmComplicationService.requestUpdate(app) }
                    }
                }
            } finally { pending.finish() }
        }
    }

    companion object {
        const val ACTION_OPTION = "it.pixelbox.cmwatch.OPTION"
        const val ACTION_REPLY = "it.pixelbox.cmwatch.REPLY"
        const val ACTION_RETRY = "it.pixelbox.cmwatch.RETRY"
        const val ACTION_RESUME = "it.pixelbox.cmwatch.RESUME"
        const val ACTION_SEEN = "it.pixelbox.cmwatch.SEEN"
        const val SESSION = "session"; const val OPTION = "option"; const val OPTION_LABEL = "label"
        const val TEXT = "text"; const val CMD_ID = "cmd"; const val QUESTION_ID = "qid"
    }
}
