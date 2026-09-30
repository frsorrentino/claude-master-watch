package it.pixelbox.cmwatch.mobile

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
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
}
