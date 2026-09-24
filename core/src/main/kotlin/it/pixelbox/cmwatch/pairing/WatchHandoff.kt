package it.pixelbox.cmwatch.pairing

import it.pixelbox.cmwatch.crypto.Pairing
import java.security.KeyPair

/**
 * Il lato orologio del passaggio (design 24/09): `hello` con un altro progetto Firebase chiede il riavvio; altrimenti una
 * chiave temporanea X25519, tenuta in memoria per 5 minuti, che apre la K mandata dal telefono.
 */
class WatchHandoff(
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val newKeyPair: () -> KeyPair = { Pairing.newKeyPair() },
    private val ttlMs: Long = 5 * 60_000L,
) {
    private var eph: KeyPair? = null
    private var ephAt = 0L
    private var uid: String? = null

    /** Firebase si riavvia solo se ne gira già uno su un altro progetto: un topic nuovo non basta. */
    fun needsRestart(active: FirebaseConfig?, incoming: FirebaseConfig): Boolean = active != null && !active.sameProject(incoming)

    /** Una sessione nuova per l'orologio con questo uid: la chiave pubblica temporanea della risposta a `hello`. */
    @Synchronized fun open(uid: String): String {
        val kp = newKeyPair()
        eph = kp; ephAt = nowMs(); this.uid = uid
        return Pairing.publicB64(kp)
    }

    /** K dal `key` del telefono. La sessione si chiude comunque: un secondo tentativo riparte da `hello`. */
    @Synchronized fun take(req: KeyRequest): ByteArray {
        val kp = eph; val u = uid
        eph = null; uid = null
        if (kp == null || u == null || nowMs() - ephAt > ttlMs) throw HandoffException(HandoffMessages.ERR_NO_SESSION)
        return Handoff.open(req.box, kp.private, req.eph, u)
    }
}
