package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.data.PendingStatus
import kotlinx.serialization.Serializable

/**
 * Un messaggio mandato dal telefono a una sessione (prompt o risposta libera), come resta nella chat della scheda.
 * `startedAt`/`doneAt`: il turno che l'ha preso, come l'ha visto il telefono; l'esito si salva quando è di quel turno.
 */
@Serializable data class Sent(
    val id: String, val session: String, val text: String, val sentAt: Long,
    val startedAt: Long? = null, val doneAt: Long? = null, val outcomeShort: String? = null, val outcomeFull: String? = null,
    /** La copia locale dell'immagine allegata, per l'anteprima nel fumetto; null senza allegato. */
    val attachment: String? = null,
)

/**
 * Lo stato dei messaggi della chat (design 30/09): dalla conferma del comando e dai turni della sessione. Lo stato del
 * PC dice solo il turno di adesso, quindi inizio e fine si registrano sul messaggio man mano che si vedono.
 */
object ChatRules {
    enum class Status { SENDING, FAILED, DELIVERED, QUEUED, WORKING, DONE }

    /** Scarto tollerato fra l'orologio del telefono (`sentAt`) e quello del PC (`turnStarted`, esito). */
    const val SKEW_S = 10L
    const val KEEP_S = 7 * 86_400L

    fun status(m: Sent, pending: PendingStatus?, result: CmdResult?, s: Session?): Status = when {
        pending == PendingStatus.FAILED || result?.ok == false -> Status.FAILED
        m.doneAt != null -> Status.DONE
        m.startedAt != null -> Status.WORKING
        result == null && pending != null -> Status.SENDING
        s?.state == SessionState.BUSY && (s.turnStarted ?: Long.MAX_VALUE) < m.sentAt - SKEW_S -> Status.QUEUED
        else -> Status.DELIVERED
    }

    fun advance(m: Sent, s: Session?, now: Long): Sent {
        if (m.doneAt != null || s == null) return m
        val from = m.sentAt - SKEW_S
        val running = s.state == SessionState.BUSY || s.state == SessionState.AWAITING
        val out = s.outcome
        return when {
            m.startedAt == null && s.state == SessionState.BUSY && (s.turnStarted ?: Long.MIN_VALUE) >= from ->
                m.copy(startedAt = s.turnStarted)
            m.startedAt != null && !running -> {
                val mine = out?.takeIf { it.at >= m.startedAt }
                m.copy(doneAt = now, outcomeShort = mine?.short, outcomeFull = mine?.full)
            }
            // Turno partito e finito fra due stati: lo dice solo l'esito, più nuovo dell'invio.
            m.startedAt == null && !running && out != null && out.at >= from ->
                m.copy(startedAt = m.sentAt, doneAt = out.at, outcomeShort = out.short, outcomeFull = out.full)
            else -> m
        }
    }

    fun prune(list: List<Sent>, now: Long): List<Sent> = list.filter { it.sentAt >= now - KEEP_S }
}
