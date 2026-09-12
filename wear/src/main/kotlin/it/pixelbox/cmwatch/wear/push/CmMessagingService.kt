package it.pixelbox.cmwatch.wear.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.pixelbox.cmwatch.wear.CmApp

/** FCM data message {kind, session, ts, key} → WakeWorker (expedited). */
class CmMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        android.util.Log.i("cmwatch", "fcm message: ${message.data}")
        WakeWorker.enqueue(this)
    }
    override fun onNewToken(token: String) { (application as CmApp).subscribeTopic() }
}
