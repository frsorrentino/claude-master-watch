package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Franz, 08/10 18:06: chiusa una sessione, la sua pagina spariva e compariva la home; «non voglio pagine che scompaiono».
// Variante A (18:29): un pannello sopra la pagina dice che si sta chiudendo, poi che è chiusa, e dopo 3 s si torna alla home.
class CloseSplashTest {
    private fun s(n: String, st: SessionState, since: Long = 100) = Session(id = n, name = n, account = "personale", project = n, state = st, since = since)
    private val sent = CloseSplash.Leaving("kb", sentAt = 1_000, cmd = "c1")

    @Test fun aLiveSessionWithoutExitShowsNothing() =
        assertNull(CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), leaving = null, lastSeen = null, now = 1_010))

    @Test fun afterExitTheSessionIsClosingUntilThePcConfirms() {
        val p = CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), sent, lastSeen = null, now = 1_002, delivered = false)
        assertEquals(CloseSplash.Phase.Closing("kb", sentAt = 1_000, delivered = false, slow = false), p)
        val d = CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), sent, lastSeen = null, now = 1_004, delivered = true)
        assertEquals(CloseSplash.Phase.Closing("kb", sentAt = 1_000, delivered = true, slow = false), d)
    }

    // Dal vivo 08/10 19:29: dopo /exit Claude Code chiedeva cosa fare dei lavori in background, e il pannello copriva la
    // domanda. Finché la sessione chiede, il pannello si fa da parte; risposto, torna.
    @Test fun aQuestionAfterExitShowsTheQuestionNotThePanel() {
        val q = it.pixelbox.cmwatch.contract.Question(
            id = "q1", kind = it.pixelbox.cmwatch.contract.QuestionKind.ASK, text = "Background tasks are running",
            options = listOf(it.pixelbox.cmwatch.contract.Option(3, "Stay")), tier = it.pixelbox.cmwatch.contract.Tier.LOW, askedAt = 1_010,
        )
        assertNull(CloseSplash.of("kb", listOf(s("kb", SessionState.WAITING).copy(question = q)), sent, lastSeen = null, now = 1_030, delivered = true))
    }

    @Test fun aClosingThatTakesLongerThanUsualSaysSo() {
        val p = CloseSplash.of("kb", listOf(s("kb", SessionState.IDLE)), sent, lastSeen = null, now = 1_000 + CloseSplash.SLOW_S, delivered = true)
        assertEquals(true, (p as CloseSplash.Phase.Closing).slow)
    }

    @Test fun goneAfterMyExitIsClosedByMeAtItsLastSighting() =
        assertEquals(
            CloseSplash.Phase.Closed("kb", at = 1_003, byMe = true),
            CloseSplash.of("kb", listOf(s("kb", SessionState.GONE, since = 1_003)), sent, lastSeen = null, now = 1_005),
        )

    @Test fun vanishedAfterMyExitIsClosedByMe() =
        assertEquals(
            CloseSplash.Phase.Closed("kb", at = 1_003, byMe = true),
            CloseSplash.of("kb", emptyList(), sent, lastSeen = s("kb", SessionState.GONE, since = 1_003), now = 1_005),
        )

    // Chiusa per conto suo resta con «Riapri» (03/10 19:57); quando esce dallo stato, il pannello dice che non l'hai chiusa tu.
    @Test fun goneOnItsOwnStaysAsAPage() =
        assertNull(CloseSplash.of("kb", listOf(s("kb", SessionState.GONE)), leaving = null, lastSeen = null, now = 1_005))

    @Test fun vanishedOnItsOwnIsClosedNotByMe() =
        assertEquals(
            CloseSplash.Phase.Closed("kb", at = 1_005, byMe = false),
            CloseSplash.of("kb", emptyList(), leaving = null, lastSeen = s("kb", SessionState.IDLE, since = 900), now = 1_005),
        )

    @Test fun anotherSessionsExitDoesNotCount() =
        assertNull(CloseSplash.of("atlas", listOf(s("atlas", SessionState.IDLE)), sent, lastSeen = null, now = 1_005))

    @Test fun neverSeenAndMissingFallsBackToNow() =
        assertEquals(CloseSplash.Phase.Closed("kb", at = 1_005, byMe = false), CloseSplash.of("kb", emptyList(), null, lastSeen = null, now = 1_005))
}
