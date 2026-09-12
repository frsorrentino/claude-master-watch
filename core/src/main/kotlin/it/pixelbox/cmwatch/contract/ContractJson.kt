package it.pixelbox.cmwatch.contract

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

object ContractJson {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = true; encodeDefaults = true }
    fun decodeState(raw: String): State = json.decodeFromString(State.serializer(), raw)
    fun decodeEvents(raw: String): List<Event> = json.decodeFromString(ListSerializer(Event.serializer()), raw)
    fun encode(cmd: Cmd): String = json.encodeToString(Cmd.serializer(), cmd)
    fun decodeResult(raw: String): CmdResult = json.decodeFromString(CmdResult.serializer(), raw)
}
