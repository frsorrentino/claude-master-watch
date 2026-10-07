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
    /** Il motivo per cui non è stato consegnato, salvato quando si vede (revisione 30/09); null finché va bene. */
    val failed: String? = null,
    /**
     * Invio programmato (piano 30/09, Task 4): l'ora in cui deve partire. Finché `sentAt` è prima di quest'ora il messaggio
     * aspetta; quando parte `sentAt` diventa l'ora vera dell'invio e il messaggio prosegue come gli altri.
     */
    val scheduledFor: Long? = null,
    /** Il pannello che un comando slash ha aperto sul PC (`Slash.panel`), salvato quando arriva; null senza. */
    val panel: String? = null,
    /** Un file allegato (non un'immagine): quello che serve a «Riprova» per rimandarlo (07/10 20:21); null senza. */
    val file: SentFile? = null,
)

/**
 * La copia di un file allegato, nella cache dell'app, con il nome, il tipo e il testo scritto con lui. Nella cache perché
 * un file può arrivare a 5,6 MB e il sistema la può svuotare; se manca, «Riprova» chiede di allegarlo di nuovo.
 */
@Serializable data class SentFile(val path: String, val name: String, val mime: String, val text: String)

/**
 * Lo stato dei messaggi della chat (design 30/09): dalla conferma del comando e dai turni della sessione. Lo stato del
 * PC dice solo il turno di adesso, quindi inizio e fine si registrano sul messaggio man mano che si vedono.
 */
object ChatRules {
    /**
     * I passaggi di un invio, nell'ordine (Franz, 30/09 22:13: lo stato preciso in tempo reale): senza rete, caricamento
     * dell'immagine, scrittura sul canale, inviato al PC, consegnato alla sessione, in coda dietro un turno, preso in
     * carico, elaborato; oppure non consegnato, con il motivo quando c'è.
     */
    enum class Status { SCHEDULED, OFFLINE, UPLOADING, SENDING, SENT, UNCERTAIN, FAILED, DELIVERED, QUEUED, WORKING, DONE }

    /** Il caricamento dell'immagine di un messaggio (contratto 1.19). */
    sealed interface Upload {
        data object Going : Upload
        data class Failed(val reason: String) : Upload
    }

    /** Scarto tollerato fra l'orologio del telefono (`sentAt`) e quello del PC (`turnStarted`, esito). */
    const val SKEW_S = 10L
    const val KEEP_S = 7 * 86_400L
    /** Oltre questo tempo dall'invio un turno nuovo non è più di quel messaggio (revisione 30/09). */
    const val CLAIM_S = 1_800L

    fun status(m: Sent, pending: PendingStatus?, result: CmdResult?, s: Session?, upload: Upload? = null): Status = when {
        m.failed != null || upload is Upload.Failed || result?.ok == false -> Status.FAILED
        waiting(m) -> Status.SCHEDULED
        upload == Upload.Going -> Status.UPLOADING
        m.doneAt != null -> Status.DONE
        m.startedAt != null -> Status.WORKING
        // Nessuna risposta in 20 s: spesso il messaggio è arrivato lo stesso (dal vivo 30/09 23:00). «In attesa del PC».
        pending == PendingStatus.FAILED && result == null -> Status.UNCERTAIN
        result == null && pending == PendingStatus.QUEUED -> Status.OFFLINE
        result == null && pending == PendingStatus.SENT -> Status.SENT
        result == null && pending != null -> Status.SENDING
        s?.state == SessionState.BUSY && (s.turnStarted ?: Long.MAX_VALUE) < m.sentAt - SKEW_S -> Status.QUEUED
        else -> Status.DELIVERED
    }

    /** Perché non è stato consegnato: il rifiuto del caricamento o del PC; null se il PC non ha risposto in tempo. */
    fun reason(pending: PendingStatus?, result: CmdResult?, upload: Upload?, m: Sent? = null): String? = when {
        m?.failed != null -> m.failed
        upload is Upload.Failed -> upload.reason
        result?.ok == false -> result.text.takeIf { it.isNotBlank() }
        else -> null
    }

    fun advance(m: Sent, s: Session?, now: Long): Sent {
        if (m.doneAt != null || m.failed != null || s == null || waiting(m)) return m
        val from = m.sentAt - SKEW_S
        val until = m.sentAt + CLAIM_S
        // Una domanda di permesso a metà turno non lo chiude (revisione 30/09).
        val running = s.state == SessionState.BUSY || s.state == SessionState.AWAITING || s.state == SessionState.WAITING
        val out = s.outcome
        return when {
            m.startedAt == null && s.state == SessionState.BUSY && (s.turnStarted ?: Long.MIN_VALUE) in from..until ->
                m.copy(startedAt = s.turnStarted)
            // Il relay mette la sessione in «awaiting» appena consegna il prompt: il turno è suo da lì.
            m.startedAt == null && s.state == SessionState.AWAITING && now <= until -> m.copy(startedAt = now)
            m.startedAt != null && !running -> {
                val mine = out?.takeIf { it.at >= m.startedAt }
                m.copy(doneAt = now, outcomeShort = mine?.short, outcomeFull = mine?.full)
            }
            // Turno partito e finito fra due stati: lo dice solo l'esito, più nuovo dell'invio.
            m.startedAt == null && !running && out != null && out.at in from..until ->
                m.copy(startedAt = m.sentAt, doneAt = out.at, outcomeShort = out.short, outcomeFull = out.full)
            else -> m
        }
    }

    /** Programmato e non ancora partito. */
    fun waiting(m: Sent): Boolean = m.scheduledFor != null && m.sentAt < m.scheduledFor

    /**
     * I programmati da mandare adesso: l'ora è passata, anche da molto (telefono spento o senza rete all'ora giusta).
     * Chi li manda aggiorna `sentAt`, e da lì non tornano più qui: partono una volta sola.
     */
    fun due(list: List<Sent>, now: Long): List<Sent> = list.filter { waiting(it) && it.scheduledFor!! <= now }

    /** «Manda stanotte»: la cartella del progetto della sessione fra quelle che il PC conosce; null se non c'è. */
    fun nightDir(state: it.pixelbox.cmwatch.contract.State, s: Session): String? =
        state.projects.firstOrNull { it.path == s.project || it.path.endsWith("/" + s.project) }?.path

    fun prune(list: List<Sent>, now: Long): List<Sent> = list.filter { it.sentAt >= now - KEEP_S }
}
