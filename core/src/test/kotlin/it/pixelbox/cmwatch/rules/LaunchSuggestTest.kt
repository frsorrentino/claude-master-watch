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
}
