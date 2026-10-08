package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Night

/**
 * La scheda Stanotte della home (mockup approvato da Franz l'08/10 alle 20:43, «povera e non mostra info utili»): i lavori
 * della coda, quello in corso per primo con da quanti minuti, gli altri nell'ordine in cui sono stati aggiunti; ognuno con il
 * progetto e il testo del lavoro. L'ultima notte la aggiunge la home dal rapporto (`NightPage`).
 */
object TonightCard {
    data class Item(val id: String, val name: String, val prompt: String, val runningMin: Int?)
    data class Model(val items: List<Item>, val running: Int, val queued: Int)

    fun of(night: Night, now: Long): Model {
        val all = night.items.orEmpty()
        val items = all.sortedWith(compareBy({ it.started == null }, { it.added })).map { i ->
            Item(i.id, i.name, i.prompt.trim(), i.started?.let { ((now - it) / 60).coerceAtLeast(0).toInt() })
        }
        val running = items.count { it.runningMin != null }
        return Model(items, running, items.size - running)
    }
}
