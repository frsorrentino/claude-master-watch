package it.pixelbox.cmwatch.mobile.push

import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.rules.PhonePrimary
import org.junit.Assert.*
import org.junit.Test

class ReplyRouteTest {
    @Test fun optionAction() = assertEquals(ReplyRoute.Cmd.Option("kb", 2), ReplyRoute.from(ReplyRoute.OPTION, "kb", 2, null))
    @Test fun textAction() = assertEquals(ReplyRoute.Cmd.Text("kb", "sì"), ReplyRoute.from(ReplyRoute.REPLY, "kb", 0, " sì "))
    @Test fun blankTextIsNothing() = assertNull(ReplyRoute.from(ReplyRoute.REPLY, "kb", 0, "  "))
    @Test fun missingSessionIsNothing() = assertNull(ReplyRoute.from(ReplyRoute.OPTION, null, 1, null))

    private val question = Question("q1", QuestionKind.ASK, "Quale?", listOf(Option(1, "A")), Tier.MEDIUM, 0)
    private fun s(q: Question?) = Session(id = "1", name = "kb", account = "personale", project = "p", state = SessionState.WAITING, since = 0, question = q)

    @Test fun textAnswersAnOpenQuestion() = assertEquals(PhonePrimary.Target.ANSWER_TEXT, ReplyRoute.textTarget(s(question), "B"))
    /** Revisione 29/09: la domanda si è chiusa sul PC con la notifica ancora aperta: il testo diventa un prompt. */
    @Test fun textBecomesPromptWhenQuestionClosed() = assertEquals(PhonePrimary.Target.PROMPT, ReplyRoute.textTarget(s(null).copy(state = SessionState.IDLE), "B"))
    @Test fun textForUnknownSessionIsPrompt() = assertEquals(PhonePrimary.Target.PROMPT, ReplyRoute.textTarget(null, "B"))
}
