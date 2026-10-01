package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import org.junit.Assert.*
import org.junit.Test

/** Le frasi rapide (piano 30/09, Task 6): le tre più usate nel progetto della sessione, almeno due volte ciascuna. */
class QuickPhrasesTest {
    private fun s(name: String, project: String) = Session(id = name, name = name, account = "personal", project = project, state = SessionState.IDLE, since = 0)
    private val kb = s("kb", "personal/kb")
    private val kb2 = s("kb-2", "personal/kb")
    private val other = s("atlas", "personal/atlas")
    private val state = State(v = 1, ts = 0, host = "pc", sessions = listOf(kb, kb2, other))
    private var t = 0L
    private fun m(session: String, text: String) = Sent("m${++t}", session, text, sentAt = t)

    @Test fun topThreeByUseForTheProject() {
        val sent = listOf(
            m("kb", "continua"), m("kb-2", "Continua "), m("kb", "continua"),
            m("kb", "esegui i test"), m("kb", "esegui i test"),
            m("kb", "fai il commit"), m("kb-2", "fai il commit"),
            m("kb", "push"), m("kb", "push"),
            m("atlas", "deploy"), m("atlas", "deploy"), m("atlas", "deploy"),
        )
        val out = QuickPhrases.of(sent, state, kb, suggestion = null)
        assertEquals(3, out.size)
        assertEquals("continua", out[0])
        assertFalse("deploy" in out)
    }

    @Test fun ignoresOneOffs() {
        assertTrue(QuickPhrases.of(listOf(m("kb", "una volta sola")), state, kb, null).isEmpty())
    }

    @Test fun noDuplicatesOfTheSuggestion() {
        val sent = listOf(m("kb", "continua"), m("kb", "continua"), m("kb", "push"), m("kb", "push"))
        assertEquals(listOf("push"), QuickPhrases.of(sent, state, kb, suggestion = "Continua"))
    }

    // Segnalazione 01/10 18:11: «non vedo aggiornamento terminale in tempo reale», un vecchio messaggio, compariva come
    // frase rapida e usciva dallo schermo. Una frase rapida è corta; lo stesso invio ripetuto (stesso id) conta una volta.
    @Test fun longMessagesAreNotQuickPhrases() {
        val long = "non vedo aggiornamento terminale in tempo reale"
        assertTrue(QuickPhrases.of(listOf(m("kb", long), m("kb", long)), state, kb, null).isEmpty())
    }

    @Test fun theSameSendCountsOnce() {
        val once = Sent("x", "kb", "continua", sentAt = 1)
        assertTrue(QuickPhrases.of(listOf(once, once.copy(sentAt = 2)), state, kb, null).isEmpty())
    }
}
