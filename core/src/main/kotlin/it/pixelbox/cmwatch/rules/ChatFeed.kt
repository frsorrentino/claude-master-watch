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
        /** Due o più passaggi di fila fra due messaggi (Franz, 01/10 15:59): una card chiusa che si apre al tocco. */
        data class Steps(val entries: List<TranscriptEntry>) : Item {
            /** Quanti passaggi per strumento, dal più usato: «7 Bash · 2 Read»; gli MCP col nome corto. */
            val counts: List<Pair<String, Int>>
                get() = entries.groupingBy { it.tool?.let(ToolText::short) ?: "?" }.eachCount().entries
                    .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).map { it.key to it.value }
        }
    }

    /** I passaggi consecutivi diventano un gruppo; uno solo resta una riga. */
    fun group(items: List<Item>): List<Item> {
        val out = mutableListOf<Item>()
        var run = mutableListOf<TranscriptEntry>()
        fun flush() {
            when (run.size) {
                0 -> Unit
                1 -> out += Item.Tool(run.single())
                else -> out += Item.Steps(run.toList())
            }
            run = mutableListOf()
        }
        items.forEach { if (it is Item.Tool) run += it.entry else { flush(); out += it } }
        flush()
        return out
    }

    enum class Page { FRESH, AFTER, BEFORE }

    const val PAGE = 50

    fun arg(lastId: String?): String = if (lastId == null) "$PAGE" else "$PAGE:after=$lastId"
    fun olderArg(firstId: String): String = "$PAGE:before=$firstId"

    /**
     * Una pagina nuova sulla lista che c'è: `AFTER` in coda, `BEFORE` in testa, `FRESH` al posto. Una voce già presente
     * si sostituisce con la sua versione nuova (una voce «in coda» che diventa normale, il costo del turno arrivato dopo):
     * mai due voci con lo stesso id.
     */
    fun append(old: List<TranscriptEntry>, page: TranscriptPage, mode: Page): List<TranscriptEntry> {
        val byId = page.entries.associateBy { it.id }
        val updated = old.map { byId[it.id] ?: it }
        val seen = old.map { it.id }.toSet()
        val fresh = page.entries.filter { it.id !in seen }
        return when (mode) {
            Page.FRESH -> page.entries
            Page.AFTER -> updated + fresh
            Page.BEFORE -> fresh + updated
        }
    }

    /**
     * Da dove ripartire con `after`: prima della prima voce ancora in coda, così la si vede cambiare; senza code dalla
     * penultima, così l'ultima si rilegge quando le arriva il costo del turno. Null = prima lettura.
     */
    fun anchor(entries: List<TranscriptEntry>): String? {
        if (entries.isEmpty()) return null
        val q = entries.indexOfFirst { it.queued }
        val i = if (q >= 0) q - 1 else entries.size - 2
        return entries.getOrNull(i.coerceAtLeast(0))?.id?.takeIf { i >= 0 } ?: entries.first().id
    }

    fun merge(entries: List<TranscriptEntry>, sent: List<Pair<Sent, ChatRules.Status>>, more: Boolean = false): List<Item> {
        val left = sent.sortedBy { it.first.sentAt }.toMutableList()
        val items = entries.map { e ->
            when (e.role) {
                "user" -> {
                    // Il relay antepone al prompt le sue istruzioni: basta che la voce finisca con il testo mandato.
                    val t = e.text?.trim().orEmpty()
                    // Solo le voci arrivate dal telefono (o da un relay che non lo dice), mai i messaggi falliti; fra più
                    // candidati con lo stesso testo quello più vicino nel tempo (revisione 30/09).
                    val at = e.at ?: Long.MAX_VALUE
                    val hit = if (e.origin != null && e.origin != "phone") null else left
                        .filter { (m, st) ->
                            st != ChatRules.Status.FAILED && m.failed == null && !ChatRules.waiting(m) && m.text.isNotBlank() && t.endsWith(m.text.trim()) &&
                                at >= m.sentAt - ChatRules.SKEW_S
                        }
                        .minByOrNull { (m, _) -> kotlin.math.abs(at - m.sentAt) }
                    // La voce nella trascrizione prova che il messaggio è arrivato, anche senza il risultato del comando.
                    if (hit != null) {
                        left.remove(hit)
                        val st = if (hit.second in listOf(ChatRules.Status.UNCERTAIN, ChatRules.Status.SENT, ChatRules.Status.SENDING)) ChatRules.Status.DELIVERED else hit.second
                        Item.Mine(hit.first, st, e)
                    } else Item.User(e)
                }
                "tool" -> Item.Tool(e)
                else -> Item.Claude(e)
            }
        }
        // I messaggi non ancora nella trascrizione, al loro orario fra le voci. Con pagine più vecchie ancora da caricare,
        // quelli più vecchi della pagina aspettano la loro pagina invece di accumularsi in testa.
        val result = items.toMutableList()
        val first = entries.firstOrNull()?.at
        left.filterNot { (m, _) -> more && first != null && m.sentAt < first }.forEach { (m, st) ->
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
        is Item.Steps -> i.entries.first().at ?: 0
    }
}
