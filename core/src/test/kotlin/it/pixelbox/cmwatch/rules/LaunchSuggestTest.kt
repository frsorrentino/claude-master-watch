package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchSuggestTest {
    private val st = State(v = 1, ts = 0, host = "pc", projects = listOf(
        Project("/a/kb", "kb", "personale", lastUsed = 50), Project("/a/docs", "docs", "personale", lastUsed = 90),
        Project("/w/kb", "kb", "lavoro", lastUsed = 99), Project("/a/old", "old", "personale", lastUsed = null),
    ))

    @Test fun onlyThatAccountMostRecentFirst() =
        assertEquals(listOf("/a/docs", "/a/kb", "/a/old"), LaunchSuggest.projects(st, "personale", "").map { it.path })

    @Test fun typedFiltersByNamePiece() =
        assertEquals(listOf("/a/kb"), LaunchSuggest.projects(st, "personale", "K").map { it.path })

    private val many = State(v = 1, ts = 0, host = "pc", projects = listOf(
        Project("/p/atlas-shop", "atlas-shop", "personal", lastUsed = 10),
        Project("/p/data-tools", "data-tools", "personal", lastUsed = 99),
        Project("/w/clients/ledger-api", "ledger-api", "work", lastUsed = 50),
        Project("/w/field-notes", "field-notes", "work", lastUsed = 70),
    ))

    @Test fun prefixBeforeContains() =
        assertEquals(listOf("atlas-shop", "data-tools"), LaunchSuggest.ranked(many, "at").map { it.name })

    @Test fun pathMatchesLast() = assertEquals(listOf("ledger-api"), LaunchSuggest.ranked(many, "clients").map { it.name })

    @Test fun bothAccountsWhenNoFilter() =
        assertEquals(setOf("personal", "work"), LaunchSuggest.ranked(many, "").map { it.account }.toSet())

    @Test fun accountFilterStillWorks() =
        assertEquals(listOf("field-notes", "ledger-api"), LaunchSuggest.ranked(many, "", account = "work").map { it.name })

    @Test fun emptyGivesMostRecent() =
        assertEquals(listOf("data-tools", "field-notes"), LaunchSuggest.ranked(many, "", limit = 2).map { it.name })

    @Test fun caseAndSpacesIgnored() = assertEquals(listOf("atlas-shop"), LaunchSuggest.ranked(many, "  Atlas ").map { it.name })

    // Segnalazione 01/10 23:20: «mostra di default 3 sessioni che sembrano casuali e cercando master non appare nulla».
    // I recenti sono solo i progetti con una data d'uso; la ricerca trova anche le sessioni per nome (la master gira in
    // `workspaces`, che non è fra i progetti).
    @Test fun recentOnlyWithADate() {
        assertEquals(listOf("/a/docs", "/a/kb"), LaunchSuggest.recent(st, "personale").map { it.path })
        assertEquals(listOf("/w/kb", "/a/docs", "/a/kb"), LaunchSuggest.recent(st, null).map { it.path })
    }

    @Test fun sessionsByName() {
        val s = State(v = 1, ts = 0, host = "pc", sessions = listOf(
            Session(id = "1", name = "claude-master", account = "personale", project = "claude-master", state = SessionState.IDLE, since = 0),
            Session(id = "2", name = "master", account = "personale", project = "workspaces", state = SessionState.GONE, since = 0),
            Session(id = "3", name = "kb", account = "personale", project = "kb", state = SessionState.IDLE, since = 0),
        ))
        assertEquals(listOf("master", "claude-master"), LaunchSuggest.sessions(s, "master").map { it.name })
        assertEquals(emptyList<String>(), LaunchSuggest.sessions(s, "").map { it.name })
    }

    // Contratto 1.26 (dal vivo 02/10 17:10: /state porta 5 progetti su 98): con l'elenco completo chiesto al PC si cerca
    // anche fra i progetti che lo stato ha tagliato, e i recenti vengono da lì.
    @Test fun searchAndRecentUseTheFullListWhenThereIsOne() {
        val full = many.projects + Project("/w/zeta-site", "zeta-site", "work", lastUsed = 5)
        assertEquals(listOf("zeta-site"), LaunchSuggest.ranked(many, "zeta", pool = full).map { it.name })
        assertEquals("zeta-site", LaunchSuggest.recent(many, null, pool = full).last().name)
        assertEquals(emptyList<String>(), LaunchSuggest.ranked(many, "zeta").map { it.name })
    }
}
