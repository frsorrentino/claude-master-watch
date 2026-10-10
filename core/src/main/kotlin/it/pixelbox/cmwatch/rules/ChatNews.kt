package it.pixelbox.cmwatch.rules

/**
 * L'apertura di una sessione (Franz, 10/10 16:01: «per il testo della conversazione va bene a+b»): le ultime voci entrano a
 * cascata dall'alto (A), e quelle arrivate dall'ultima volta che la sessione era aperta sono «Nuovi», con una riga sopra la
 * prima e un fondo appena tinto che sfuma (B).
 */
object ChatNews {
    /** Quante voci, contando dal fondo, entrano a cascata: più o meno quelle sullo schermo all'apertura. */
    const val CASCADE = 6
    /** Lo scarto fra una voce e la successiva: con l'entrata di 200 ms la cascata finisce col volo della card. */
    const val STAGGER_MS = 30L

    /** Il momento di una voce: la trascrizione, o l'invio per un tuo messaggio non ancora trascritto. */
    fun at(i: ChatFeed.Item): Long? = when (i) {
        is ChatFeed.Item.Mine -> i.entry?.at ?: i.sent.sentAt
        is ChatFeed.Item.User -> i.entry.at
        is ChatFeed.Item.Claude -> i.entry.at
        is ChatFeed.Item.Tool -> i.entry.at
        is ChatFeed.Item.Steps -> i.entries.mapNotNull { it.at }.maxOrNull()
    }

    /** L'ultimo momento della conversazione, da ricordare quando si lascia la pagina. */
    fun latest(items: List<ChatFeed.Item>): Long? = items.mapNotNull(::at).maxOrNull()

    /**
     * L'indice della prima voce nuova: più recente di `seenAt` e non tua. Null se non c'è niente di nuovo, o se la sessione
     * non era mai stata aperta, perché allora tutto sarebbe nuovo.
     */
    fun firstNew(items: List<ChatFeed.Item>, seenAt: Long?): Int? {
        if (seenAt == null) return null
        return items.indexOfFirst { it !is ChatFeed.Item.Mine && (at(it) ?: 0L) > seenAt }.takeIf { it >= 0 }
    }

    /** Il posto nella cascata, dall'alto, delle ultime `CASCADE` voci; null per le altre, che non si muovono. */
    fun rank(index: Int, size: Int): Int? = (index - (size - CASCADE).coerceAtLeast(0)).takeIf { it >= 0 && index < size }
}
