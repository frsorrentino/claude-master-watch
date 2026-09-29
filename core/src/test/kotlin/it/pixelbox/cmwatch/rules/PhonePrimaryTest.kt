package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.*
import org.junit.Assert.*
import org.junit.Test

class PhonePrimaryTest {
    private val q = Question("q1", QuestionKind.ASK, "Quale?", listOf(Option(1, "A")), Tier.MEDIUM, 0)
    private fun s(st: SessionState, question: Question? = null) =
        Session(id = "1", name = "kb", account = "personale", project = "p", state = st, since = 0, question = question)

    @Test fun emptyDraftNoButtonExceptClosed() {
        assertEquals(PhonePrimary.Button.NONE, PhonePrimary.button(s(SessionState.IDLE), "  "))
        assertEquals(PhonePrimary.Button.REOPEN, PhonePrimary.button(s(SessionState.GONE), ""))
    }

    @Test fun draftGivesSend() = assertEquals(PhonePrimary.Button.SEND, PhonePrimary.button(s(SessionState.IDLE), "vai"))

    @Test fun closedSessionNeverSends() {
        assertEquals(PhonePrimary.Button.REOPEN, PhonePrimary.button(s(SessionState.GONE), "vai"))
        assertNull(PhonePrimary.target(s(SessionState.GONE), "vai"))
    }

    @Test fun textGoesToAnswerOnlyWhileQuestionExists() {
        assertEquals(PhonePrimary.Target.ANSWER_TEXT, PhonePrimary.target(s(SessionState.WAITING, q), "B"))
        // La domanda si è chiusa sul PC mentre il campo era pieno: il testo diventa un prompt, non una risposta a vuoto.
        assertEquals(PhonePrimary.Target.PROMPT, PhonePrimary.target(s(SessionState.IDLE), "B"))
    }
}
