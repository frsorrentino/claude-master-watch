package it.pixelbox.cmwatch.mobile.pair

import android.content.Context
import it.pixelbox.cmwatch.pairing.FirebaseBoot
import it.pixelbox.cmwatch.pairing.FirebaseConfig
import it.pixelbox.cmwatch.transport.FirebaseAuthToken
import it.pixelbox.cmwatch.transport.Rtdb

class SdkPhoneFirebase(private val ctx: Context) : PhoneFirebase {
    override suspend fun ensure(cfg: FirebaseConfig): Ensure {
        val active = FirebaseBoot.active
        if (active != null && !active.sameProject(cfg)) return Ensure.Restart
        if (active == null && FirebaseBoot.start(ctx, cfg) == null) return Ensure.Failed
        FirebaseAuthToken.token() ?: return Ensure.Failed
        return FirebaseAuthToken.uid()?.let { Ensure.Ready(it) } ?: Ensure.Failed
    }

    override fun rtdb(): Rtdb = Rtdb(FirebaseBoot.active!!.databaseUrl.removeSuffix("/"), token = { FirebaseAuthToken.token() })
}
