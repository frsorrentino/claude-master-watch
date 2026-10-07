package it.pixelbox.cmwatch.rules

/**
 * La doppia conferma della modalità live (specifica live, §7), per gli ok e per i comandi che nel contratto chiedono una
 * conferma (chiusure, cancellazioni). Primo tocco «Approva»: la voce rilegge per intero cosa e dove. Poi un tasto diverso
 * dal primo tenuto premuto per 2 secondi, entro 10 secondi dal primo tocco. La seconda conferma non si dà mai a voce:
 * qui entrano solo pressioni di tasti. L'ora arriva da fuori, in millisecondi, come la conta delle vibrazioni.
 */
object DoubleConfirm {
    /** Quanto si tiene premuto il secondo tasto. */
    const val HOLD_MS = 2_000L

    /** Entro quanto, dal primo tocco, la pressione deve arrivare ai 2 secondi. */
    const val TIMEOUT_MS = 10_000L

    enum class Phase { IDLE, ARMED, CONFIRMING, SENT, CANCELLED }

    /** Perché una conferma è annullata; la voce dice «annullato» in tutti i casi. */
    enum class Why { TIMEOUT, TAP, VANISHED, REFUSED }

    /**
     * `target` = il compito da approvare (o la sessione da chiudere); `firstKey` = il tasto del primo tocco; `holdKey` e
     * `holdFrom` = la pressione in corso del secondo tasto, null quando non c'è.
     */
    data class Machine(
        val phase: Phase = Phase.IDLE, val target: String? = null, val firstKey: String? = null, val armedAt: Long = 0,
        val holdKey: String? = null, val holdFrom: Long = 0, val why: Why? = null,
    )

    /** Il tocco «Approva»: da ferma, inviata o annullata si arma; durante una conferma non cambia nulla. */
    fun arm(m: Machine, target: String, key: String, nowMs: Long): Machine =
        if (m.phase == Phase.ARMED || m.phase == Phase.CONFIRMING) m else Machine(Phase.ARMED, target, key, nowMs)

    /** Comincia a premere un tasto: conta solo se è diverso da quello del primo tocco. */
    fun press(m: Machine, key: String, nowMs: Long): Machine =
        if (m.phase != Phase.ARMED || key == m.firstKey) m else m.copy(holdKey = key, holdFrom = nowMs)

    /** Rilascia: una pressione arrivata a 2 secondi entro la scadenza conferma; una più corta si ricomincia. */
    fun release(m: Machine, key: String, nowMs: Long): Machine {
        if (m.phase != Phase.ARMED || key != m.holdKey) return m
        val next = tick(m, nowMs)
        return if (next.phase == Phase.ARMED) next.copy(holdKey = null, holdFrom = 0) else next
    }

    /**
     * Il passare del tempo, a ogni conteggio della vibrazione. Conta l'istante in cui la pressione arriva a 2 secondi,
     * non quello del tick, che può arrivare in ritardo. `present` = la richiesta è ancora in `approvals`: se sparisce
     * prima dell'invio si annulla; dopo l'invio sparisce proprio perché l'ok è arrivato.
     */
    fun tick(m: Machine, nowMs: Long, present: Boolean = true): Machine {
        if (m.phase != Phase.ARMED) return m
        if (!present) return cancelled(m, Why.VANISHED)
        val deadline = m.armedAt + TIMEOUT_MS
        val held = m.holdKey?.let { m.holdFrom + HOLD_MS }
        return when {
            held != null && held <= deadline && nowMs >= held -> m.copy(phase = Phase.CONFIRMING)
            nowMs >= deadline -> cancelled(m, Why.TIMEOUT)
            else -> m
        }
    }

    /** Il tasto «Annulla». */
    fun cancel(m: Machine): Machine = if (m.phase == Phase.ARMED) cancelled(m, Why.TAP) else m

    /** La risposta del relay all'`approve`: inviato, oppure annullato col suo rifiuto. */
    fun result(m: Machine, ok: Boolean): Machine = when {
        m.phase != Phase.CONFIRMING -> m
        ok -> m.copy(phase = Phase.SENT)
        else -> cancelled(m, Why.REFUSED)
    }

    private fun cancelled(m: Machine, why: Why) = m.copy(phase = Phase.CANCELLED, why = why, holdKey = null, holdFrom = 0)
}
