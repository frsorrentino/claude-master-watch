package it.pixelbox.cmwatch.mobile.pair

import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import it.pixelbox.cmwatch.mobile.PhoneApp
import it.pixelbox.cmwatch.mobile.live.LiveService
import it.pixelbox.cmwatch.rules.LiveWire
import kotlinx.coroutines.launch

/** L'orologio torna raggiungibile: se gli manca ancora K, gliela si consegna anche ad app chiusa. */
class PhoneListenerService : WearableListenerService() {
    /** I tocchi della scheda live sul watch: li prende `LiveService`, se è acceso. */
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == LiveWire.TAP) runCatching { LiveService.taps.tryEmit(LiveWire.tap(event.data)) }
    }

    override fun onCapabilityChanged(info: CapabilityInfo) {
        if (info.nodes.isEmpty()) return
        val app = application as PhoneApp
        app.scope.launch { app.pairing.completePending() }
    }
}
