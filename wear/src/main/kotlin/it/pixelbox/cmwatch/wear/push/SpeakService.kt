package it.pixelbox.cmwatch.wear.push

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.wear.CmApp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Tutte le letture a voce passano da qui, dall'app e dalle notifiche: il servizio resta in primo piano finché il ▶
 * prepara o parla, così una lettura lunga continua a schermo spento (Franz, 14/09: «leggere testo lungo senza doverlo
 * guardare»). Un secondo tocco ferma; finita la lettura il servizio si toglie da solo.
 */
class SpeakService : Service() {
    private var watch: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as CmApp
        ServiceCompat.startForeground(this, NOTIF_ID, notification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        when (intent?.action) {
            ACTION_STOP -> app.reader.stop()
            ACTION_LAST -> intent.getStringExtra(SESSION)?.let { app.reader.toggleLast(it, intent.getStringExtra(TEXT)) }
            ACTION_TEXT -> intent.getStringExtra(TEXT)?.let { app.reader.toggleText(it) }
        }
        watch?.cancel()
        if (!app.reader.busy) { finish(); return START_NOT_STICKY }
        // Partita da una notifica: lì «Leggi» diventa «Ferma» finché la voce va (Franz, 14/09 16:08).
        intent?.getStringExtra(FROM)?.let { app.notifier.reading(it, app.repo.snapshot.value.state?.sessions.orEmpty()) }
        watch = app.scope.launch {
            combine(app.reader.preparing, app.speaker.speaking) { p, s -> p || s }.first { !it }
            finish()
        }
        return START_NOT_STICKY
    }

    private fun finish() {
        val app = application as CmApp
        app.notifier.reading(null, app.repo.snapshot.value.state?.sessions.orEmpty())
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(): Notification = NotificationCompat.Builder(this, Notifier.CHANNEL_FOLLOW)
        .setSmallIcon(R.drawable.ic_app_mono)
        .setContentTitle(getString(R.string.tts_reading))
        .setOngoing(true).setSilent(true).setLocalOnly(true)
        .addAction(
            R.drawable.ic_play, getString(R.string.tts_stop),
            PendingIntent.getService(this, 1, Intent(this, SpeakService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE),
        )
        .build()

    companion object {
        const val TEXT = "text"
        const val SESSION = "session"
        const val ACTION_LAST = "it.pixelbox.cmwatch.speak.LAST"
        const val ACTION_TEXT = "it.pixelbox.cmwatch.speak.TEXT"
        const val ACTION_STOP = "it.pixelbox.cmwatch.speak.STOP"
        private const val NOTIF_ID = 7301

        /** Sessione della notifica da cui parte la lettura; null se parte dall'app. */
        const val FROM = "from"

        fun lastIntent(ctx: Context, session: String, fallback: String?, from: String? = null): Intent =
            Intent(ctx, SpeakService::class.java).setAction(ACTION_LAST).putExtra(SESSION, session).putExtra(TEXT, fallback).putExtra(FROM, from)

        fun textIntent(ctx: Context, text: String, from: String? = null): Intent =
            Intent(ctx, SpeakService::class.java).setAction(ACTION_TEXT).putExtra(TEXT, text).putExtra(FROM, from)

        fun last(ctx: Context, session: String, fallback: String?) = ContextCompat.startForegroundService(ctx, lastIntent(ctx, session, fallback))
        fun text(ctx: Context, text: String) = ContextCompat.startForegroundService(ctx, textIntent(ctx, text))
    }
}
