package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/** Rotte del telefono (design 29/09) e quelle che uno stato riempie: in Demo devono esserci tutte. */
object PhoneRoutes {
    enum class Route { SESSIONS, SHEET_QUESTION, SHEET_IDLE, TERMINAL, LAUNCH, DIARY, SETTINGS }

    fun reachable(state: State): Set<Route> = buildSet {
        add(Route.SETTINGS)
        if (state.sessions.isNotEmpty()) { add(Route.SESSIONS); add(Route.TERMINAL) }
        if (state.sessions.any { it.question != null }) add(Route.SHEET_QUESTION)
        if (state.sessions.any { it.state == SessionState.IDLE && it.outcome != null }) add(Route.SHEET_IDLE)
        if (state.projects.isNotEmpty()) add(Route.LAUNCH)
        if (state.recap.items.isNotEmpty() || state.night.queued > 0) add(Route.DIARY)
    }
}
