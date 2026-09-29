package it.pixelbox.cmwatch.mobile.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.pixelbox.cmwatch.mobile.PhoneApp
import kotlinx.coroutines.launch

/** La sveglia FCM: un GET dello stato; le notifiche partono dal diff degli stati in PhoneApp. */
class PhoneMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val app = application as PhoneApp
        app.scope.launch { app.repo.refresh() }
    }
}
