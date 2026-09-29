package it.pixelbox.cmwatch.mobile.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.R
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * La sveglia FCM: un GET dello stato; le notifiche partono dal diff degli stati in PhoneApp. Il GET si attende qui
 * (onMessageReceived gira su un thread suo, con circa 10 s): tornando subito il processo poteva morire prima (revisione 29/09).
 */
class PhoneMessagingService : FirebaseMessagingService() {
    /** Token nuovo: l'iscrizione al topic si rifà, come sull'orologio. */
    override fun onNewToken(token: String) { (application as PhoneApp).subscribeTopic() }

    override fun onMessageReceived(message: RemoteMessage) {
        val app = application as PhoneApp
        runBlocking { withTimeoutOrNull(8_000) { app.repo.refresh() } }
        // Contratto 1.18: diario e resoconto della notte arrivano con il push; si avvisa in silenzio, il testo è nel Diario.
        when (message.data["kind"]) {
            "recap" -> app.notifier.diary(getString(R.string.notif_diary))
            "night_report" -> app.notifier.diary(getString(R.string.notif_night))
        }
    }
}
