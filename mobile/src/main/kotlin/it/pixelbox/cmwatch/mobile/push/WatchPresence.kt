package it.pixelbox.cmwatch.mobile.push

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/** L'orologio con l'app è raggiungibile adesso? (capacità «cmwatch_wear», come l'accoppiamento). */
class WatchPresence(private val ctx: Context) {
    suspend fun reachable(): Boolean = runCatching {
        Wearable.getCapabilityClient(ctx).getCapability("cmwatch_wear", CapabilityClient.FILTER_REACHABLE).await().nodes.isNotEmpty()
    }.getOrDefault(false)
}
