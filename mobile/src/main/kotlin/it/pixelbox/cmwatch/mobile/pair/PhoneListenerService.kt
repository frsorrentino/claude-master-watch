package it.pixelbox.cmwatch.mobile.pair

import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.WearableListenerService
import it.pixelbox.cmwatch.mobile.PhoneApp
import kotlinx.coroutines.launch

/** L'orologio torna raggiungibile: se gli manca ancora K, gliela si consegna anche ad app chiusa. */
class PhoneListenerService : WearableListenerService() {
    override fun onCapabilityChanged(info: CapabilityInfo) {
        if (info.nodes.isEmpty()) return
        val app = application as PhoneApp
        app.scope.launch { app.pairing.completePending() }
    }
}
