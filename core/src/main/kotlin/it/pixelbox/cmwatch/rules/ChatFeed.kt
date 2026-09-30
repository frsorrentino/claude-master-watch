package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.TranscriptEntry
import it.pixelbox.cmwatch.contract.TranscriptPage

/**
 * La chat della scheda sessione con la conversazione vera (contratto 1.22, design 30/09): le voci di `transcript` più i
 * messaggi mandati dal telefono, che prendono il posto della loro voce e portano il loro stato. Un messaggio non ancora
 * nella trascrizione si mette al suo orario.
 */
object ChatFeed {
    sealed interface Item {
        /** Un messaggio mandato dal telefono; `entry` = la sua voce nella trascrizione, null finché non c'è. */
        data class Mine(val sent: Sent, val status: ChatRules.Status, val entry: TranscriptEntry?) : Item
        data class User(val entry: TranscriptEntry) : Item
        data class Claude(val entry: TranscriptEntry) : Item
        data class Tool(val entry: TranscriptEntry) : Item
    }

    enum class Page { FRESH, AFTER, BEFORE }

    const val PAGE = 50

    fun arg(lastId: String?): String = if (lastId == null) "$PAGE" else "$PAGE:after=$lastId"
    fun olderArg(firstId: String): String = "$PAGE:before=$firstId"

    /** Una pagina nuova sulla lista che c'è: `AFTER` in coda, `BEFORE` in testa, `FRESH` al posto; mai due voci con lo stesso id. */
    fun append(old: List<TranscriptEntry>, page: TranscriptPage, mode: Page): List<TranscriptEntry> {
        val seen = old.map { it.id }.toSet()
        val fresh = page.entries.filter { it.id !in seen }
        return when (mode) {
            Page.FRESH -> page.entries
            Page.AFTER -> old + fresh
            Page.BEFORE -> fresh + old
        }
    }

    fun merge(entries: List<TranscriptEntry>, sent: List<Pair<Sent, ChatRules.Status>>): List<Item> {
        val left = sent.sortedBy { it.first.sentAt }.toMutableList()
        val items = entries.map { e ->
            when (e.role) {
                "user" -> {
                    // Il relay antepone al prompt le sue istruzioni: basta che la voce finisca con il testo mandato.
                    val t = e.text?.trim().orEmpty()
                    // Una voce scritta al PC non è mai un messaggio del telefono, anche con lo stesso testo.
                    val hit = if (e.origin == "pc") null else left.firstOrNull { (m, _) ->
                        m.text.isNotBlank() && t.endsWith(m.text.trim()) && (e.at ?: Long.MAX_VALUE) >= m.sentAt - ChatRules.SKEW_S
                    }
                    if (hit != null) { left.remove(hit); Item.Mine(hit.first, hit.second, e) } else Item.User(e)
                }
                "tool" -> Item.Tool(e)
                else -> Item.Claude(e)
            }
        }
        // I messaggi non ancora nella trascrizione, al loro orario fra le voci.
        val result = items.toMutableList()
        left.forEach { (m, st) ->
            val at = result.indexOfFirst { atOf(it) > m.sentAt }
            val item = Item.Mine(m, st, null)
            if (at < 0) result.add(item) else result.add(at, item)
        }
        return result
    }

    private fun atOf(i: Item): Long = when (i) {
        is Item.Mine -> i.entry?.at ?: i.sent.sentAt
        is Item.User -> i.entry.at ?: 0
        is Item.Claude -> i.entry.at ?: 0
        is Item.Tool -> i.entry.at ?: 0
    }
}
