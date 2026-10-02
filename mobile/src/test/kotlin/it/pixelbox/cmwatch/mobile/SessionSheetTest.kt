package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import org.junit.Test
import it.pixelbox.cmwatch.contract.*
import it.pixelbox.cmwatch.mobile.ui.*
import java.io.File

class SessionSheetTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(locale = "it"), theme = "android:Theme.Material.NoActionBar")
    private val st = ContractJson.decodeState(File("../contract/state-1-question.json").readText())
    private val none = SheetActions({}, {}, { _, _ -> }, {}, {}, {}, {}, {}, {})

    @Test fun sheetQuestion() = paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(st.sessions.first { it.question != null }, st.ts, emptyList(), 120, none) } }
    @Test fun sheetIdleWithOutcome() = paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(st.sessions.first { it.state == SessionState.IDLE }, st.ts, emptyList(), 120, none) } }
    @Test fun sheetClosed() = paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(st.sessions.first().copy(state = SessionState.GONE, question = null), st.ts, emptyList(), 120, none) } }
    @Test fun sheetHighTier() = paparazzi.snapshot {
        val s = st.sessions.first { it.question != null }
        CmPhoneTheme(still = true) { SessionSheet(s.copy(question = s.question!!.copy(tier = Tier.HIGH)), st.ts, emptyList(), 120, none) }
    }
    @Test fun sheetBusyWithGoal() = paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(st.sessions.first { it.goal != null }, st.ts, emptyList(), 120, none) } }

    // L'avviso delle altre sessioni sotto la barra (Franz, 02/10 20:47, variante A): chi ti aspetta e chi ha finito.
    @Test fun sheetElsewhereWaiting() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { SessionSheet(st.sessions.first { it.state == SessionState.IDLE }, st.ts, emptyList(), 120, none, elsewhere = it.pixelbox.cmwatch.rules.Elsewhere.Waiting(listOf("ledger-api"))) }
    }
    @Test fun sheetElsewhereFinished() = paparazzi.snapshot {
        CmPhoneTheme(still = true) { SessionSheet(st.sessions.first { it.state == SessionState.IDLE }, st.ts, emptyList(), 120, none, elsewhere = it.pixelbox.cmwatch.rules.Elsewhere.Finished("atlas-shop", st.ts - 30)) }
    }

    // La chat dei messaggi mandati nei sei stati (design 30/09, parte 3), con l'esito del turno elaborato.
    @Test fun sheetChat() = paparazzi.snapshot {
        val s = st.sessions.first { it.state == SessionState.IDLE }
        val t = st.ts - 3000
        val rows = listOf(
            ChatRow(it.pixelbox.cmwatch.rules.Sent("1", s.name, "Run the tests and tell me what fails", t, startedAt = t + 5, doneAt = t + 200, outcomeFull = "All 40 tests green. Nothing failed."), it.pixelbox.cmwatch.rules.ChatRules.Status.DONE),
            ChatRow(it.pixelbox.cmwatch.rules.Sent("2", s.name, "Now update the changelog", t + 300), it.pixelbox.cmwatch.rules.ChatRules.Status.WORKING),
            ChatRow(it.pixelbox.cmwatch.rules.Sent("3", s.name, "And bump the version", t + 320), it.pixelbox.cmwatch.rules.ChatRules.Status.QUEUED),
            ChatRow(it.pixelbox.cmwatch.rules.Sent("4", s.name, "Tag it", t + 330), it.pixelbox.cmwatch.rules.ChatRules.Status.DELIVERED),
            ChatRow(it.pixelbox.cmwatch.rules.Sent("5", s.name, "Push", t + 340), it.pixelbox.cmwatch.rules.ChatRules.Status.SENDING),
            ChatRow(it.pixelbox.cmwatch.rules.Sent("6", s.name, "And the release notes", t + 350), it.pixelbox.cmwatch.rules.ChatRules.Status.FAILED),
        )
        CmPhoneTheme(still = true) { SessionSheet(s, st.ts, emptyList(), 120, none, chat = rows, choices = st.choices, ops = st.ops, canAttach = true) }
    }

    // La conversazione vera (contratto 1.22), come la pagina della fixture, con un messaggio del telefono agganciato.
    @Test fun sheetTranscript() = paparazzi.snapshot {
        val t = st.ts - 600
        val entries = listOf(
            TranscriptEntry("u1.0", "user", text = "Add the Tuesday meeting notes to the draft", at = t, origin = "pc"),
            TranscriptEntry("a1.0", "assistant", text = "I'll read the draft first.", at = t + 5),
            TranscriptEntry("a1.1", "tool", text = "docs/draft.md", at = t + 5, tool = "Read"),
            TranscriptEntry("a3.0", "tool", text = "grep -n Tuesday notes/*.md", at = t + 20, tool = "Bash", note = "Find the Tuesday notes", error = true),
            TranscriptEntry("a4.0", "assistant", text = "The notes file was missing, so I added the Tuesday section to the draft by hand.", at = t + 40, turn = TranscriptTurn(t, t + 45, 4020, 130)),
            TranscriptEntry("a6.0", "tool", text = "docs/cover.png", at = t + 152, tool = "Write", files = listOf(TranscriptFile("/w/field-notes/docs/cover.png", "image/png", 48_000))),
            TranscriptEntry("a7.0", "tool", text = "Tuesday minutes", at = t + 155, tool = "SendUserFile", files = listOf(TranscriptFile("/w/field-notes/docs/minutes.pdf", "application/pdf", 212_000))),
            TranscriptEntry("p1.0", "user", text = "Also add the attendees list", at = t + 158, origin = "phone"),
        )
        val mine = it.pixelbox.cmwatch.rules.Sent("c1", "field-notes", "Also add the attendees list", t + 156)
        val feed = it.pixelbox.cmwatch.rules.ChatFeed.merge(entries, listOf(mine to it.pixelbox.cmwatch.rules.ChatRules.Status.WORKING))
        val s = st.sessions.first { it.state == SessionState.BUSY }
        CmPhoneTheme(still = true) { SessionSheet(s, st.ts, emptyList(), 120, none, choices = st.choices, ops = st.ops, canAttach = true, feed = feed, more = true) }
    }

    // Stop al posto di Invia mentre la sessione lavora e il campo è vuoto (contratto 1.21).
    @Test fun sheetBusyStop() = paparazzi.snapshot {
        val s = st.sessions.first { it.state == SessionState.BUSY }
        CmPhoneTheme(still = true) { SessionSheet(s, st.ts, emptyList(), 120, none, choices = st.choices, ops = st.ops) }
    }

    /** Finestra al 94 %: la riga dell'avviso sopra la barra, e un messaggio programmato alla ripartenza nella chat. */
    @Test fun sheetQuotaWarning() {
        val s = st.sessions.first { it.state == SessionState.IDLE }
        val reset = st.ts + 3 * 3600
        val later = it.pixelbox.cmwatch.rules.Sent("m-later", s.name, "Run the nightly import again", st.ts - 60, scheduledFor = reset)
        paparazzi.snapshot {
            CmPhoneTheme(still = true) {
                SessionSheet(s, st.ts, emptyList(), 120, none,
                    chat = listOf(ChatRow(later, it.pixelbox.cmwatch.rules.ChatRules.Status.SCHEDULED)),
                    quota = it.pixelbox.cmwatch.rules.QuotaWarning.Warn(s.account, 94, reset, projected = false))
            }
        }
    }

    /** Sessione ferma con le frasi rapide del progetto sopra la barra. */
    @Test fun sheetQuickPhrases() {
        val s = st.sessions.first { it.state == SessionState.IDLE }.copy(suggestion = null)
        paparazzi.snapshot { CmPhoneTheme(still = true) { SessionSheet(s, st.ts, emptyList(), 120, none, phrases = listOf("continua", "esegui i test", "fai il commit e il push")) } }
    }

    // Casa A (mockup approvato da Franz, 02/10 07:38): esito della master con i consigli, Per te, In corso, quota.
    @Test fun masterHomeA() = paparazzi.snapshot {
        val m = st.sessions.first { it.state == SessionState.IDLE }.copy(name = "master", question = null)
        val reply = TranscriptEntry("a1", "assistant", "Lanciata claude-master sulla fase 2.2. Solo commit locali: push e release con il tuo ok.\n\nEsito: Fase 2.2 avviata su claude-master\nProssimi: distilla il confronto nella kb · prova la casa dal vivo", st.ts - 600)
        val hero = it.pixelbox.cmwatch.rules.MasterHome.hero(listOf(reply), m)
        val q = st.sessions.first { it.question != null }
        // Variante 3 di «Per te» (Franz, 02/10 21:11): chi ti aspetta e chi ha finito, righe chiuse.
        val rows = listOf(
            it.pixelbox.cmwatch.rules.MasterHome.Row(it.pixelbox.cmwatch.rules.MasterHome.Kind.QUESTION, q.name, q.question!!.text, session = q.name, at = q.question!!.askedAt),
            it.pixelbox.cmwatch.rules.MasterHome.Row(it.pixelbox.cmwatch.rules.MasterHome.Kind.FINISHED, "atlas-shop", FINISHED_TEXT, session = "atlas-shop", at = st.ts - 900),
        )
        val rings = listOf(
            it.pixelbox.cmwatch.rules.PhoneOverview.Ring("personale", true, 3, 11, null, null),
            it.pixelbox.cmwatch.rules.PhoneOverview.Ring("professionale", false, 0, 63, null, null, stale = true),
        )
        CmPhoneTheme(still = true) {
            SessionSheet(m, st.ts, emptyList(), 120, none, grid = true, home = { _, _ ->
                HeroCard(hero, m, {}, {}, {}, {})
                ForYouCard(rows, onAction = {}, now = st.ts, working = it.pixelbox.cmwatch.rules.MasterHome.working(st.copy(sessions = st.sessions + m)))
                QuotaBars(rings, onOpen = {})
            })
        }
    }

    // Le tabelle nel testo (Franz, 02/10 21:11): una stretta resta griglia, una larga diventa una scheda per riga.
    @Test fun sheetTables() = paparazzi.snapshot {
        val s = st.sessions.first { it.state == SessionState.IDLE }
        val t = st.ts - 600
        val reply = "I file toccati:\n\n| file | righe |\n|---|---|\n| Repo.kt | 336 |\n| Slash.kt | 47 |\n\nI numeri\n\n" +
            "| cosa | ora | a mezzogiorno |\n|---|---|---|\n| post del film (01/10) | 223 visualizzazioni, 4 repost | 121 visualizzazioni, 1 repost |\n| nuovi follower | 1 | — |"
        val rows = listOf(ChatRow(it.pixelbox.cmwatch.rules.Sent("1", s.name, "Dammi i numeri", t, startedAt = t + 5, doneAt = t + 60, outcomeFull = reply), it.pixelbox.cmwatch.rules.ChatRules.Status.DONE))
        CmPhoneTheme(still = true) { SessionSheet(s, st.ts, emptyList(), 120, none, chat = rows) }
    }

    // Le righe aperte: la domanda con le opzioni (la prima piena) e il turno finito con i consigli.
    @Test fun forYouOpenRows() = paparazzi.snapshot {
        val q = st.sessions.first { it.question != null }
        CmPhoneTheme(still = true) {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.background(it.pixelbox.cmwatch.ui.tokens.CmColors.bg).padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                AttentionRow(it.pixelbox.cmwatch.rules.MasterHome.Row(it.pixelbox.cmwatch.rules.MasterHome.Kind.QUESTION, q.name, q.question!!.text, session = q.name, at = q.question!!.askedAt),
                    st.ts, open = true, onToggle = {}, question = q.question, onAnswer = {}, onStep = {}, onOpen = {})
                AttentionRow(it.pixelbox.cmwatch.rules.MasterHome.Row(it.pixelbox.cmwatch.rules.MasterHome.Kind.FINISHED, "atlas-shop", FINISHED_TEXT, session = "atlas-shop", at = st.ts - 900),
                    st.ts, open = true, onToggle = {}, question = null, onAnswer = {}, onStep = {}, onOpen = {})
            }
        }
    }

    private companion object {
        const val FINISHED_TEXT = "Test verdi, 40 su 40. Changelog aggiornato.\nProssimi: tagga la v2.8.0 · apri la PR"
    }
}
