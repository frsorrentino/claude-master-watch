package it.pixelbox.cmwatch.data

import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.ChatRules
import it.pixelbox.cmwatch.rules.Sent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * I messaggi mandati dal telefono, per la chat della scheda sessione (design 30/09): su file JSON per 7 giorni. Un file
 * assente o rovinato vale una chat vuota; la scrittura passa da un file temporaneo, così un'interruzione non lo rovina.
 */
class ChatLog(private val file: File, private val now: () -> Long) {
    private val serializer = ListSerializer(Sent.serializer())
    private val _messages = MutableStateFlow(load())
    val messages: StateFlow<List<Sent>> = _messages

    private fun load(): List<Sent> =
        runCatching { ContractJson.json.decodeFromString(serializer, file.readText()) }.getOrDefault(emptyList())

    @Synchronized private fun save(list: List<Sent>) {
        _messages.value = list
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeText(ContractJson.json.encodeToString(serializer, list))
        tmp.renameTo(file)
    }

    @Synchronized fun add(m: Sent) = save(_messages.value.filter { it.id != m.id } + m)

    /** A ogni stato: i passaggi dei messaggi della sessione, poi via quelli più vecchi di 7 giorni. */
    @Synchronized fun advance(state: State?) {
        val t = now()
        val sessions = state?.sessions.orEmpty().associateBy { it.name }
        val next = ChatRules.prune(_messages.value.map { m -> ChatRules.advance(m, sessions[m.session], t) }, t)
        if (next != _messages.value) save(next)
    }

    fun forSession(name: String): List<Sent> = _messages.value.filter { it.session == name }.sortedBy { it.sentAt }
}
