package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.State

/** La coda «Ti aspettano» (piano 30/09, Task 1): le domande aperte di tutte le sessioni, da rispondere in fila. */
object AttentionQueue {
    data class Item(val session: String, val questionId: String, val askedAt: Long)

    /** Le domande aperte, dalla più vecchia. */
    fun items(state: State): List<Item> = state.sessions
        .mapNotNull { s -> s.question?.let { Item(s.name, it.id, it.askedAt) } }
        .sortedBy { it.askedAt }

    /**
     * La domanda dopo `current`; dopo l'ultima si riparte dalla più vecchia. Se `current` non c'è più (risposta al PC
     * mentre la coda era aperta) si va alla più vecchia rimasta: mai una risposta a una domanda sparita.
     */
    fun next(state: State, current: String?): Item? {
        val all = items(state)
        val i = all.indexOfFirst { it.questionId == current }
        return if (i < 0) all.firstOrNull() else all.getOrNull(i + 1) ?: all.firstOrNull()
    }

    /** La pagina della domanda `questionId` nella fila di adesso; null se non c'è più. */
    fun page(items: List<Item>, questionId: String?): Int? = items.indexOfFirst { it.questionId == questionId }.takeIf { it >= 0 }
}
