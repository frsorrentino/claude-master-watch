package it.pixelbox.cmwatch.wear.push

import android.app.Service
import android.content.Intent
import android.os.IBinder
import it.pixelbox.cmwatch.wear.CmApp

/** «Leggi» dalla notifica: TTS senza aprire l'app; un secondo tocco ferma. */
class SpeakService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val app = application as CmApp
        val text = intent?.getStringExtra(TEXT)
        if (app.speaker.speaking.value || text.isNullOrBlank()) app.speaker.stop() else app.speaker.speak(text)
        stopSelf(startId)
        return START_NOT_STICKY
    }
    companion object { const val TEXT = "text" }
}
