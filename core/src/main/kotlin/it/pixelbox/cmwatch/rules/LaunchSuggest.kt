package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Project
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.State

/** I progetti da proporre nel foglio «Lancia»: dell'account scelto, per pezzo di nome, i più recenti prima. */
object LaunchSuggest {
    fun projects(state: State, account: String, typed: String, limit: Int = 6): List<Project> {
        val t = typed.trim().lowercase()
        return state.projects
            .filter { it.account == account && (t.isEmpty() || t in it.name.lowercase()) }
            .sortedByDescending { it.lastUsed ?: Long.MIN_VALUE }
            .take(limit)
    }

    /**
     * La ricerca con completamento di «Lancia» (Franz, 30/09): prima i nomi che iniziano con il testo, poi quelli che lo
     * contengono, poi quelli che lo hanno nella cartella; a pari merito il più recente. `account` null = tutti e due.
     * `pool`: l'elenco completo chiesto al PC (contratto 1.26), altrimenti i progetti di /state, tagliati a 5.
     */
    fun ranked(state: State, typed: String, account: String? = null, limit: Int = 6, pool: List<Project> = state.projects): List<Project> {
        val t = typed.trim().lowercase()
        fun rank(p: Project): Int? = when {
            t.isEmpty() -> 0
            p.name.lowercase().startsWith(t) -> 0
            t in p.name.lowercase() -> 1
            t in p.path.lowercase() -> 2
            else -> null
        }
        return pool.filter { account == null || it.account == account }
            .mapNotNull { p -> rank(p)?.let { it to p } }
            .sortedWith(compareBy<Pair<Int, Project>> { it.first }.thenByDescending { it.second.lastUsed ?: Long.MIN_VALUE })
            .map { it.second }
            .take(limit)
    }

    /**
     * I recenti del foglio a campo vuoto: solo i progetti con una data d'uso, i più recenti prima (segnalazione 01/10
     * 23:20: senza data l'ordine sembrava casuale). `account` null = tutti e due; `pool` come in `ranked`.
     */
    fun recent(state: State, account: String?, limit: Int = 6, pool: List<Project> = state.projects): List<Project> =
        pool.filter { (account == null || it.account == account) && it.lastUsed != null }
            .sortedByDescending { it.lastUsed }.take(limit)

    /**
     * Le sessioni con il testo nel nome, prima quelle che cominciano così (segnalazione 01/10 23:20: «master» non trovava
     * nulla, perché la master gira in `workspaces`, che non è fra i progetti). Una chiusa si riapre, una aperta si apre.
     */
    fun sessions(state: State, typed: String, limit: Int = 3): List<Session> {
        val t = typed.trim().lowercase()
        if (t.isEmpty()) return emptyList()
        return state.sessions.filter { t in it.name.lowercase() }
            .sortedWith(compareBy<Session> { if (it.name.lowercase().startsWith(t)) 0 else 1 }.thenBy { it.name })
            .take(limit)
    }
}
