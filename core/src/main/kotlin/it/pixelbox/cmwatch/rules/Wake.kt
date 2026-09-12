package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/** Cosa fare quando arriva un nuovo /state (sveglia FCM o stream): notifiche solo per ciò che è cambiato, poi tile e complication. */
object Wake {
    enum class NotifyKind { QUESTION, OUTCOME, GONE, QUOTA }

    sealed class Action {
        data class Notify(val kind: NotifyKind, val session: String?) : Action()
        data object RefreshTile : Action()
        data object RefreshComplications : Action()
    }

    fun plan(prev: State?, cur: State, quotaThreshold: Int = 95): List<Action> {
        val out = ArrayList<Action>()
        val before = prev?.sessions?.associateBy { it.name }.orEmpty()
        for (s in cur.sessions) {
            val p = before[s.name]
            s.question?.let { q -> if (p?.question?.id != q.id) out += Action.Notify(NotifyKind.QUESTION, s.name) }
            s.outcome?.let { o -> if (s.followed && p?.outcome?.at != o.at) out += Action.Notify(NotifyKind.OUTCOME, s.name) }
            if (s.state == SessionState.GONE && p != null && p.state != SessionState.GONE) out += Action.Notify(NotifyKind.GONE, s.name)
        }
        val names = cur.sessions.map { it.name }.toSet()
        for (p in before.values) if (p.name !in names && p.state != SessionState.GONE) out += Action.Notify(NotifyKind.GONE, p.name)
        for ((account, q) in cur.quota) {
            val hot = maxOf(q.h5 ?: 0, q.w7 ?: 0) >= quotaThreshold
            val was = prev?.quota?.get(account)?.let { maxOf(it.h5 ?: 0, it.w7 ?: 0) >= quotaThreshold } ?: false
            if (hot && !was) out += Action.Notify(NotifyKind.QUOTA, account)
        }
        out += Action.RefreshTile
        out += Action.RefreshComplications
        return out
    }
}
