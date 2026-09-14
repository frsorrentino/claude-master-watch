package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State

/**
 * Quale account è personale (contratto 1.8, Franz, 14/09 22:55): lo dice il tipo che manda il relay, «personal» o
 * «work», non il nome, perché gli account possono chiamarsi in qualunque modo (beta pubblica S08). Senza il tipo, un
 * relay precedente alla 1.8, si ripiega sul nome «personale».
 */
object Accounts {
    const val PERSONAL = "personal"

    fun personal(name: String, kind: String?): Boolean =
        if (kind != null) kind == PERSONAL else name.equals("personale", ignoreCase = true)

    fun isPersonal(s: Session): Boolean = personal(s.account, s.accountKind)

    fun isPersonalQuota(name: String, q: QuotaAccount): Boolean = personal(name, q.kind)

    /** Il nome dell'account personale fra quelli della quota, se c'è. */
    fun personalQuota(state: State): String? = state.quota.entries.firstOrNull { (k, q) -> isPersonalQuota(k, q) }?.key

    /** L'account scelto se esiste nella quota, altrimenti quello personale, altrimenti il primo in ordine. */
    fun resolve(state: State, chosen: String): String? =
        state.quota.keys.firstOrNull { it.equals(chosen, ignoreCase = true) } ?: personalQuota(state) ?: state.quota.keys.minOrNull()
}
