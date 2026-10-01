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

    @Test fun firstOptionIsTheFilledButtonWhenTheQuestionHasOptions() =
        assertEquals(PhonePrimary.Button.OPTION, PhonePrimary.button(s(SessionState.WAITING, q), "anche testo"))

    @Test fun questionWithoutOptionsKeepsSend() =
        assertEquals(PhonePrimary.Button.SEND, PhonePrimary.button(s(SessionState.WAITING, q.copy(options = emptyList())), "B"))

    private val ops = listOf("prompt", "interrupt")

    @Test fun stopWhileWorkingWithAnEmptyField() =
        assertEquals(PhonePrimary.Composer.STOP, PhonePrimary.composer(s(SessionState.BUSY), "", ops))

    @Test fun typingTurnsStopBackIntoSend() =
        assertEquals(PhonePrimary.Composer.SEND, PhonePrimary.composer(s(SessionState.BUSY), "and also", ops))

    @Test fun noStopWithoutRelaySupport() {
        assertEquals(PhonePrimary.Composer.NONE, PhonePrimary.composer(s(SessionState.BUSY), "", null))
        assertEquals(PhonePrimary.Composer.NONE, PhonePrimary.composer(s(SessionState.BUSY), "", listOf("prompt")))
    }

    @Test fun sendIsTonalWhenTheFirstOptionIsFilled() =
        assertEquals(PhonePrimary.Composer.SEND_TONAL, PhonePrimary.composer(s(SessionState.WAITING, q), "B", ops))

    @Test fun closedSessionReopensFromTheBar() =
        assertEquals(PhonePrimary.Composer.REOPEN, PhonePrimary.composer(s(SessionState.GONE), "text", ops))

    @Test fun idleWithEmptyFieldHasNothingToDo() =
        assertEquals(PhonePrimary.Composer.NONE, PhonePrimary.composer(s(SessionState.IDLE), " ", ops))

    /**
     * Il prompt suggerito (contratto 1.23) vale solo per una sessione ferma (Franz, 01/10 14:57: dopo l'invio la
     * pillola restava sopra il campo mentre la sessione pensava, perché lo stato ottimistico teneva il suggerimento).
     */
    @Test fun suggestionOnlyForAnIdleSessionWithAnEmptyField() {
        val idle = s(SessionState.IDLE).copy(suggestion = "pubblicato, controlla")
        assertEquals("pubblicato, controlla", PhonePrimary.suggestion(idle, ""))
        assertNull(PhonePrimary.suggestion(idle.copy(state = SessionState.BUSY), ""))
        assertNull(PhonePrimary.suggestion(idle.copy(state = SessionState.AWAITING), ""))
        assertNull(PhonePrimary.suggestion(idle, "scrivo altro"))
        assertNull(PhonePrimary.suggestion(idle.copy(question = q, state = SessionState.WAITING), ""))
        assertNull(PhonePrimary.suggestion(idle.copy(suggestion = "  "), ""))
    }
}
