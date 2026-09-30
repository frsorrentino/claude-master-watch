package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Project
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
     */
    fun ranked(state: State, typed: String, account: String? = null, limit: Int = 6): List<Project> {
        val t = typed.trim().lowercase()
        fun rank(p: Project): Int? = when {
            t.isEmpty() -> 0
            p.name.lowercase().startsWith(t) -> 0
            t in p.name.lowercase() -> 1
            t in p.path.lowercase() -> 2
            else -> null
        }
        return state.projects.filter { account == null || it.account == account }
            .mapNotNull { p -> rank(p)?.let { it to p } }
            .sortedWith(compareBy<Pair<Int, Project>> { it.first }.thenByDescending { it.second.lastUsed ?: Long.MIN_VALUE })
            .map { it.second }
            .take(limit)
    }
}
