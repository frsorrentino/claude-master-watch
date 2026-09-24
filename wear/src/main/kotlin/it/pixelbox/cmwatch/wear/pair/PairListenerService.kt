package it.pixelbox.cmwatch.wear.pair

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.wearable.WearableListenerService
import it.pixelbox.cmwatch.pairing.HandoffMessages
import it.pixelbox.cmwatch.pairing.KeyResponse
import it.pixelbox.cmwatch.wear.CmApp
import kotlinx.coroutines.launch

/** Le richieste del telefono (`/cmwatch/pair/…`) arrivano anche ad app chiusa: il servizio si avvia da sé (spike C). */
class PairListenerService : WearableListenerService() {
    override fun onRequest(nodeId: String, path: String, request: ByteArray): Task<ByteArray> {
        val app = application as CmApp
        val done = TaskCompletionSource<ByteArray>()
        app.scope.launch {
            done.setResult(runCatching { app.pairReceiver.handle(path, request) }.getOrElse {
                android.util.Log.w("cmwatch", "pair request $path", it)
                HandoffMessages.encode(KeyResponse.serializer(), KeyResponse(error = HandoffMessages.ERR_BAD_REQUEST))
            })
        }
        return done.task
    }
}
