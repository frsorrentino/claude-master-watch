package it.pixelbox.cmwatch.wear.pair

import it.pixelbox.cmwatch.crypto.KeyVault
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.pairing.HandoffException
import it.pixelbox.cmwatch.pairing.HandoffMessages
import it.pixelbox.cmwatch.pairing.HelloRequest
import it.pixelbox.cmwatch.pairing.HelloResponse
import it.pixelbox.cmwatch.pairing.KeyRequest
import it.pixelbox.cmwatch.pairing.KeyResponse
import it.pixelbox.cmwatch.pairing.WatchHandoff
import it.pixelbox.cmwatch.transport.FirebaseAuthToken
import it.pixelbox.cmwatch.wear.CmApp
import it.pixelbox.cmwatch.wear.haptics.Haptics

/** Il lato orologio dell'accoppiamento dal telefono (design 24/09): risponde a `hello` e a `key`. */
class PairReceiver(private val app: CmApp, private val handoff: WatchHandoff = WatchHandoff()) {

    suspend fun handle(path: String, body: ByteArray): ByteArray = when (path) {
        HandoffMessages.HELLO -> HandoffMessages.encode(HelloResponse.serializer(), hello(body))
        HandoffMessages.KEY -> HandoffMessages.encode(KeyResponse.serializer(), key(body))
        else -> HandoffMessages.encode(KeyResponse.serializer(), KeyResponse(error = HandoffMessages.ERR_BAD_REQUEST))
    }

    private suspend fun hello(body: ByteArray): HelloResponse {
        val req = HandoffMessages.decode(HelloRequest.serializer(), body) ?: return HelloResponse(error = HandoffMessages.ERR_BAD_REQUEST)
        val incoming = req.f.config()
        if (handoff.needsRestart(FirebaseBoot.active, incoming)) {
            // Un altro progetto: si salva e si riparte; il prossimo `hello` del telefono trova Firebase già su quello nuovo.
            app.prefs.update { it.copy(firebaseJson = incoming.toJson()) }
            app.restartSoon()
            return HelloResponse(restart = true)
        }
        if (FirebaseBoot.active == null && FirebaseBoot.start(app, incoming) == null) return HelloResponse(error = HandoffMessages.ERR_AUTH)
        app.prefs.update { it.copy(firebaseJson = incoming.toJson()) }
        FirebaseBoot.retopic(incoming.topic)
        FirebaseAuthToken.token() ?: return HelloResponse(error = HandoffMessages.ERR_AUTH)
        val uid = FirebaseAuthToken.uid() ?: return HelloResponse(error = HandoffMessages.ERR_AUTH)
        return HelloResponse(uid = uid, name = app.prefs.current().deviceName, eph = handoff.open(uid))
    }

    private suspend fun key(body: ByteArray): KeyResponse {
        val req = HandoffMessages.decode(KeyRequest.serializer(), body) ?: return KeyResponse(error = HandoffMessages.ERR_BAD_REQUEST)
        val k = try { handoff.take(req) } catch (e: HandoffException) { return KeyResponse(error = e.code) }
        val wrapped = runCatching { KeyVault.wrap(k, KeyVault.keystoreKek()) }.getOrNull() ?: return KeyResponse(error = HandoffMessages.ERR_STORE)
        app.prefs.update { it.copy(paired = true, uid = FirebaseAuthToken.uid(), host = req.host, wrappedKey = wrapped) }
        app.subscribeTopic()
        app.reconfigure()
        Haptics.play(app, Haptics.Kind.CONFIRMED)
        return KeyResponse(ok = true)
    }
}
