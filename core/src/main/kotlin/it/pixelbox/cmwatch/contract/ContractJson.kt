package it.pixelbox.cmwatch.contract

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object ContractJson {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = true; encodeDefaults = true }
    fun decodeState(raw: String): State = json.decodeFromString(State.serializer(), raw)
    fun decodeEvents(raw: String): List<Event> = json.decodeFromString(ListSerializer(Event.serializer()), raw)
    fun encode(cmd: Cmd): String = json.encodeToString(Cmd.serializer(), cmd)
    fun decodeResult(raw: String): CmdResult = json.decodeFromString(CmdResult.serializer(), raw)
    /** Contratto 1.22: il `text` di un risultato `transcript` è a sua volta JSON. */
    fun decodeTranscript(raw: String): TranscriptPage = json.decodeFromString(TranscriptPage.serializer(), raw)
    /** Contratto 1.44: il rapporto della notte, il `text` dell'op `night`. */
    fun decodeNightReport(raw: String): NightReport = json.decodeFromString(NightReport.serializer(), raw)
    fun decodeAgenda(raw: String): AgendaPage = json.decodeFromString(AgendaPage.serializer(), raw)
    fun decodeAgendaSet(raw: String): AgendaSetResult = json.decodeFromString(AgendaSetResult.serializer(), raw)
    fun decodeProjects(raw: String): ProjectsPage = json.decodeFromString(ProjectsPage.serializer(), raw)
    fun decodeSearch(raw: String): SearchPage = json.decodeFromString(SearchPage.serializer(), raw)
    fun decodeTimeline(raw: String): TimelinePage = json.decodeFromString(TimelinePage.serializer(), raw)
    fun decodePairAdd(raw: String): PairAddInvite =
        json.decodeFromString(PairAddOffer.serializer(), raw).let { PairAddInvite(it.qr?.toString(), it.code, it.exp) }
}
