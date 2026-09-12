package it.pixelbox.cmwatch.wear.push

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.pixelbox.cmwatch.wear.CmApp
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** Sveglia FCM: un GET di /state, poi Room, notifiche locali (solo con app non in primo piano), tile e complication. */
class CmMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val app = application as CmApp
        val foreground = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        runBlocking { withTimeoutOrNull(8_000) { app.onWake(notify = !foreground) } }
    }

    override fun onNewToken(token: String) {
        (application as CmApp).subscribeTopic()
    }
}
