package it.pixelbox.cmwatch.wear.pair

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** «Apri sul telefono»: l'app se c'è (capacità cmwatch_phone), altrimenti la sua pagina su Play. */
object PhoneLauncher {
    private const val PHONE_CAPABILITY = "cmwatch_phone"

    suspend fun open(ctx: Context): Boolean = runCatching {
        val hasApp = Wearable.getCapabilityClient(ctx).getCapability(PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE).await().nodes.isNotEmpty()
        val uri = if (hasApp) "cmwatch://pair" else "market://details?id=${ctx.packageName}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addCategory(Intent.CATEGORY_BROWSABLE)
        withContext(Dispatchers.IO) { RemoteActivityHelper(ctx).startRemoteActivity(intent).get(20, TimeUnit.SECONDS) }
        true
    }.getOrDefault(false)
}
