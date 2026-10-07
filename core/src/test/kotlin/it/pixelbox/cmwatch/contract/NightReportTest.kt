package it.pixelbox.cmwatch.contract

import it.pixelbox.cmwatch.Fixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Il rapporto della notte (schema team-supervisor/night-report v1, op `night` del contratto 1.44): la fixture è inventata. */
class NightReportTest {
    private val r = ContractJson.decodeNightReport(Fixtures.read("night-report-sample.json"))

    @Test fun theWindowAndTheLastMessage() {
        assertEquals("2026-10-07", r.date)
        assertEquals(1791325980L, r.window.start)
        assertEquals("vado a dormire, prosegui tu e segui le sessioni", r.window.lastMessage?.text)
    }

    @Test fun theTimelineInTheFileOrder() {
        assertEquals(listOf("master", "ledger-api", "atlas-shop", "a1b2c3d4", "e5f6a7b8", "c9d0e1f2"), r.timeline.map { it.id })
        val master = r.timeline[0]
        assertEquals(NightEntry.Kind.SESSION, master.kind); assertNull(master.end); assertEquals("running", master.outcome); assertTrue(master.live)
        assertEquals(6, r.timeline[1].counts?.commits)
        val job = r.timeline[3]
        assertEquals(NightEntry.Kind.NIGHT_JOB, job.kind); assertEquals("stopped", job.outcome); assertNull(job.counts)
    }

    @Test fun whatNeedsFranz() {
        assertEquals("Pubblico la release 1.4 adesso?", r.attention.questions.single().text)
        assertEquals(2, r.attention.questions.single().options.size)
        assertEquals("Release 1.4 di ledger-api", r.attention.approvals.single().title)
        assertEquals("/clear", r.attention.unblock.single().text)
    }

    @Test fun projectsWithPartsAndEvents() {
        val p = r.projects[0]
        assertEquals(4, p.partsTotal); assertEquals(3, p.parts.count { it.state == "done" })
        assertEquals(listOf("commit", "test", "outcome"), p.events.map { it.kind })
        assertEquals(listOf("!: /clear"), r.projects[1].waitingOn)
    }

    @Test fun anUnknownFieldOrKindDoesNotBreakTheRead() {
        val more = Fixtures.read("night-report-sample.json").replace("\"kind\": \"session\"", "\"kind\": \"session\", \"extra\": 1")
            .replaceFirst("\"kind\": \"night_job\"", "\"kind\": \"robot\"")
        val x = ContractJson.decodeNightReport(more)
        assertEquals(NightEntry.Kind.OTHER, x.timeline[3].kind)
    }
}
