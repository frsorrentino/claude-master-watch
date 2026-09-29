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
}
