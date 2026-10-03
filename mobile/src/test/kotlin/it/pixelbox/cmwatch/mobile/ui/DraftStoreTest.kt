package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.runtime.saveable.SaverScope
import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La bozza sopra l'interruttore dei 840 dp (standard della master, 04/10): la finestra del Chromebook che cambia larghezza
 * ricrea l'activity e sposta la scheda in un altro punto della composizione; la bozza passa dallo stato salvato.
 */
class DraftStoreTest {
    private fun s(question: String? = null) = Session(
        id = "s1", name = "atlas-shop", account = "personal", project = "personal/atlas-shop", state = SessionState.IDLE, since = 0,
        question = question?.let { Question(it, QuestionKind.ASK, "?", emptyList(), Tier.LOW, 0) },
    )

    private fun roundTrip(d: DraftStore): DraftStore {
        val saved = with(DraftStore.Saver) { SaverScope { true }.save(d) }!!
        return DraftStore.Saver.restore(saved)!!
    }

    @Test fun theDraftSurvivesTheSavedState() {
        val d = DraftStore()
        d.state(s()).value = "run the checkout tests\nthen tag"
        assertEquals("run the checkout tests\nthen tag", roundTrip(d).state(s()).value)
    }

    // Come la bozza della scheda: una domanda nuova non eredita quella scritta prima.
    @Test fun aNewQuestionStartsEmpty() {
        val d = DraftStore()
        d.state(s()).value = "prima"
        assertEquals("", d.state(s(question = "q1")).value)
        assertEquals("prima", d.state(s()).value)
    }

    @Test fun anEmptiedDraftIsForgotten() {
        val d = DraftStore()
        d.state(s()).value = "x"
        d.state(s()).value = ""
        assertEquals("", roundTrip(d).state(s()).value)
    }
}
