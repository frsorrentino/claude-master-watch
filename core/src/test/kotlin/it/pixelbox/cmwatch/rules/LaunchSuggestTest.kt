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
}
