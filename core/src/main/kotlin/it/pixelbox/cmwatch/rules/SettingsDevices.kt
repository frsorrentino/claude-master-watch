package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/**
 * Lo schema dei dispositivi nelle Impostazioni (mockup A, scelto da Franz il 03/10 alle 21:59): telefono, PC e orologio,
 * ognuno col suo stato, e i due fili che li uniscono. Verde = vivo, arancio = dato vecchio o orologio non pronto, grigio
 * = non abbinato. Le frasi le compone l'app dalle sue stringhe.
 */
object SettingsDevices {
    enum class Tone { LIVE, STALE, OFF }

    data class Phone(val model: String, val version: String, val notifications: Boolean, val tone: Tone = Tone.LIVE)

    /** `ageMinutes`: da quanto è vecchio lo stato, null se fresco; `open`: le sessioni aperte come nella home. */
    data class Pc(val host: String?, val tone: Tone, val ageMinutes: Int?, val open: Int, val accounts: List<PhoneBoard.QuotaRow>)

    /** `reachable`: il telefono vede l'orologio adesso (Data Layer); null = non ancora saputo. */
    data class Watch(val name: String?, val keyDelivered: Boolean, val reachable: Boolean?, val tone: Tone)

    data class Model(val paired: Boolean, val phone: Phone, val pc: Pc, val watch: Watch, val pcLink: Tone, val watchLink: Tone)

    fun build(
        host: String?, state: State?, freshness: Freshness?, now: Long,
        phoneModel: String, version: String, notifications: Boolean,
        watchName: String?, watchPending: Boolean, watchReachable: Boolean?,
    ): Model {
        val paired = host != null
        val pcTone = when {
            !paired -> Tone.OFF
            state == null || freshness is Freshness.Stale -> Tone.STALE
            else -> Tone.LIVE
        }
        val open = state?.sessions?.count { it.state != SessionState.GONE && it.name != ContextActions.MASTER } ?: 0
        val pc = Pc(host, pcTone, (freshness as? Freshness.Stale)?.minutes, open, state?.let { PhoneBoard.quotaRows(it, now) }.orEmpty())
        val watchTone = when {
            !paired || watchName == null -> Tone.OFF
            watchPending || watchReachable == false -> Tone.STALE
            else -> Tone.LIVE
        }
        val watch = Watch(watchName, watchName != null && !watchPending, watchReachable, watchTone)
        return Model(paired, Phone(phoneModel, version, notifications), pc, watch, pcLink = pcTone, watchLink = watchTone)
    }
}
