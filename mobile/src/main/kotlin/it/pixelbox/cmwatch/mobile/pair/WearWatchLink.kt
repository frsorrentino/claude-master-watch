package it.pixelbox.cmwatch.mobile.pair

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.TimeUnit

class WearWatchLink(private val ctx: Context) : WatchLink {
    override suspend fun find(): WatchNode? = runCatching {
        Wearable.getCapabilityClient(ctx).getCapability(WATCH_CAPABILITY, CapabilityClient.FILTER_REACHABLE).await()
            .nodes.firstOrNull()?.let { WatchNode(it.id, it.displayName) }
    }.getOrNull()

    override suspend fun anyConnected(): WatchNode? = runCatching {
        Wearable.getNodeClient(ctx).connectedNodes.await().firstOrNull()?.let { WatchNode(it.id, it.displayName) }
    }.getOrNull()

    override suspend fun request(node: WatchNode, path: String, body: ByteArray): ByteArray =
        withTimeout(20_000) { Wearable.getMessageClient(ctx).sendRequest(node.id, path, body).await() }

    override suspend fun openPlayOnWatch(node: WatchNode): Boolean = runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${ctx.packageName}")).addCategory(Intent.CATEGORY_BROWSABLE)
        withContext(Dispatchers.IO) { RemoteActivityHelper(ctx).startRemoteActivity(intent, node.id).get(20, TimeUnit.SECONDS) }
        true
    }.getOrDefault(false)

    companion object { const val WATCH_CAPABILITY = "cmwatch_wear" }
}
