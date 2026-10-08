package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** La pagina Notte dal rapporto (specifica del 07/10, approvata alle 21:50): gli stessi casi di night.test.ts nella web. */
class NightPageTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val report = ContractJson.decodeNightReport(Fixtures.read("night-report-sample.json"))
    private val page = NightPage.of(report, zone)

    /** Franz, 08/10 12:30: «non capisco cosa è da fare o fatto»: in testa i conteggi per esito, a parole. */
    @Test fun countsByOutcomeForTheChips() {
        assertEquals(NightPage.Counts(done = 2, running = 2, stopped = 1, asking = 1), page.counts)
    }

    @Test fun theNightIsNamedByItsTwoDays() {
        assertEquals(LocalDate.of(2026, 10, 6), page.dayBefore)
        assertEquals(LocalDate.of(2026, 10, 7), page.day)
        assertTrue(page.fromLastMessage)
        assertEquals(2 * 3600 + 3 * 60, page.windowS)
    }

    @Test fun oneCardPerItemInTheFileOrder() =
        assertEquals(listOf("master", "ledger-api", "atlas-shop", "a1b2c3d4", "e5f6a7b8", "c9d0e1f2"), page.cards.map { it.id })

    @Test fun theIconSaysTheOutcomeLikeTelegram() {
        assertEquals(
            listOf(NightPage.Icon.RUNNING, NightPage.Icon.QUESTION, NightPage.Icon.OK, NightPage.Icon.STOPPED, NightPage.Icon.OK, NightPage.Icon.RUNNING),
            page.cards.map { it.icon },
        )
    }

    @Test fun aRunningItemHasNoEndAndReachesTheEndOfTheAxis() {
        val master = page.cards[0]
        assertNull(master.end); assertNull(master.durationS)
        assertEquals(0.0, master.from, 1e-9); assertEquals(1.0, master.to, 1e-9)
    }

    @Test fun aClosedItemSitsOnTheAxisWithItsDuration() {
        val atlas = page.cards[2]
        assertEquals(64 * 60, atlas.durationS)
        assertEquals(3.0 / 123, atlas.from, 1e-6)
        assertEquals(67.0 / 123, atlas.to, 1e-6)
    }

    @Test fun aOneMinuteJobStillShowsAsADot() {
        val job = page.cards[3]
        assertTrue(job.to - job.from >= NightPage.MIN_BAR - 1e-9)
    }

    @Test fun queueTitlesLoseTheFolderAndTheCutEnding() {
        val job = page.cards[3]
        assertTrue(job.queue)
        assertEquals("Prima esecuzione del lavoro notturno sul checkout", job.title)
        assertEquals("atlas-shop-notte", job.folder)
        assertFalse(job.title.contains('…'))
        assertEquals("Dati del registro dei pagamenti per la pagina mensile", page.cards[4].title)
    }

    @Test fun sessionsKeepTheirNameAndOpenTheChatOnlyWhenLive() {
        assertEquals("ledger-api", page.cards[1].title); assertNull(page.cards[1].folder)
        assertEquals("ledger-api", page.cards[1].chat)
        assertNull(page.cards[2].chat)   // chiusa: la conversazione arriva con la richiesta 2 al relay
        assertNull(page.cards[3].chat)
    }

    @Test fun theStepsAreTheProjectEventsInsideTheItem() =
        assertEquals(listOf("commit", "test", "outcome"), page.cards[1].steps.map { it.kind })

    @Test fun axisTicksOnTheFullAndHalfHours() =
        assertEquals(listOf("01:00", "01:30", "02:00", "02:30"), page.axis.map { it.label })

    @Test fun whatNeedsFranzComesFirstWithTheQuestion() {
        assertEquals(listOf(NightPage.NeedKind.QUESTION, NightPage.NeedKind.APPROVAL, NightPage.NeedKind.UNBLOCK), page.needs.map { it.kind })
        assertEquals("field-notes", page.needs[2].session)
    }

    @Test fun projectsWithTheirPartsAndWhatTheyWaitFor() {
        val ledger = page.projects[0]
        assertEquals(3, ledger.done); assertEquals(4, ledger.total)
        assertEquals(listOf("Pubblico la release 1.4 adesso?", "Release 1.4 di ledger-api"), ledger.waiting)
        assertEquals(listOf("/clear"), page.projects[1].waiting)
    }

    @Test fun shortTitlesAreLeftAlone() = assertEquals("Riordino delle note", NightPage.shortTitle("Riordino delle note"))

    @Test fun aCutTitleEndsAtTheLastWholeSentenceOrWord() {
        assertEquals("Seconda esecuzione della voce d17d7b00", NightPage.shortTitle("Seconda esecuzione della voce d17d7b00. Alle 02:01 la prima si è fermata perché le fonti stavano fuori dal worktree e i permessi ne negavano la lett…"))
        assertEquals("Un titolo lungo senza punti che il relay ha tagliato a metà di una parola", NightPage.shortTitle("Un titolo lungo senza punti che il relay ha tagliato a metà di una parola lungh…"))
    }
}
