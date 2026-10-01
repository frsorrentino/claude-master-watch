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

    /** Il motivo di un invio fallito resta sul messaggio: non si perde con i risultati in memoria né con un riavvio. */
    @Synchronized fun markFailed(id: String, reason: String) {
        val next = _messages.value.map { if (it.id == id && it.failed == null) it.copy(failed = reason) else it }
        if (next != _messages.value) save(next)
    }

    /**
     * I programmati da mandare adesso (`ChatRules.due`), segnati come partiti nello stesso passo: due giri insieme del
     * lavoro in background non li mandano due volte. Chi li riceve li manda con il loro id.
     */
    @Synchronized fun claimDue(): List<Sent> {
        val t = now()
        val due = ChatRules.due(_messages.value, t).map { it.id }.toSet()
        if (due.isEmpty()) return emptyList()
        val next = _messages.value.map { if (it.id in due) it.copy(sentAt = t) else it }
        save(next)
        return next.filter { it.id in due }
    }

    /** Un invio programmato non riuscito torna in attesa: il prossimo giro lo riprende (revisione finale 01/10). */
    @Synchronized fun unclaim(id: String) {
        val next = _messages.value.map { m -> if (m.id == id && m.scheduledFor != null) m.copy(sentAt = minOf(m.sentAt, m.scheduledFor - 1)) else m }
        if (next != _messages.value) save(next)
    }

    fun forSession(name: String): List<Sent> = _messages.value.filter { it.session == name }.sortedBy { it.sentAt }
}
