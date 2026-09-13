package it.pixelbox.cmwatch.rules

/**
 * Nomi di sessione distinguibili (Franz via la master, 13/09 09:19): «claude-master» e «claude-master-watch» troncati
 * in coda diventano lo stesso «claude-maste…». Quando due nomi visibili condividono un prefisso lungo, il taglio va in
 * mezzo, così la parte che distingue resta; altrimenti coda come sempre. L'ellissi vale solo sul nome (design, sezione 3).
 */
object NameText {
    const val MIN_SHARED = 4

    fun commonPrefix(a: String, b: String): String = a.commonPrefixWith(b)

    /** Vero se un altro nome visibile condivide abbastanza prefisso da rendere ambiguo il taglio in coda. */
    fun sharesPrefix(name: String, others: List<String>, minShared: Int = MIN_SHARED): Boolean =
        others.any { it != name && commonPrefix(it, name).length >= minShared }

    /** Nome accorciato a `max` caratteri: in mezzo se c'è collisione di prefisso, in coda altrimenti. */
    fun shorten(name: String, others: List<String>, max: Int, ellipsis: String = "…"): String {
        if (name.length <= max) return name
        if (max <= ellipsis.length) return name.take(max)
        val room = max - ellipsis.length
        return if (sharesPrefix(name, others)) {
            val tail = (room + 1) / 2
            val head = room - tail
            name.take(head) + ellipsis + name.takeLast(tail)
        } else {
            name.take(room) + ellipsis
        }
    }
}
