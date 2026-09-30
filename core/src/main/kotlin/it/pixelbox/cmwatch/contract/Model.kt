package it.pixelbox.cmwatch.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SessionState {
    @SerialName("waiting") WAITING, @SerialName("busy") BUSY, @SerialName("idle") IDLE,
    @SerialName("awaiting") AWAITING, @SerialName("gone") GONE
}

@Serializable
enum class QuestionKind { @SerialName("permission") PERMISSION, @SerialName("ask") ASK, @SerialName("plan") PLAN }

@Serializable
enum class Tier { @SerialName("low") LOW, @SerialName("medium") MEDIUM, @SerialName("high") HIGH }

@Serializable
enum class EventKind {
    @SerialName("question") QUESTION, @SerialName("answered") ANSWERED, @SerialName("outcome") OUTCOME,
    @SerialName("gone") GONE, @SerialName("launched") LAUNCHED, @SerialName("quota") QUOTA, @SerialName("resumed") RESUMED,
    /** Contratto 1.18 (R4): il diario delle 20:00 e il resoconto della notte; `ref` = giorno ISO, `session` e `account` null. */
    @SerialName("recap") RECAP, @SerialName("night_report") NIGHT_REPORT,
}

@Serializable
enum class CmdOp {
    @SerialName("answer") ANSWER, @SerialName("prompt") PROMPT, @SerialName("launch") LAUNCH,
    @SerialName("follow") FOLLOW, @SerialName("unfollow") UNFOLLOW, @SerialName("resume") RESUME,
    @SerialName("screen") SCREEN, @SerialName("allow_all") ALLOW_ALL,
    /** Contratto 1.4: l'ultima risposta intera della sessione, per la lettura a voce. */
    @SerialName("last") LAST,
    /** Contratto 1.9: una sessione gone rilanciata nella sua cartella, stessa conversazione se il PC la conosce. */
    @SerialName("reopen") REOPEN,
    /** Contratto 1.12 (in preparazione da claude-master): modello ed effort della sessione, validi solo per lei. */
    @SerialName("model") MODEL, @SerialName("effort") EFFORT,
    /** Contratto 1.17 (R3): la coda della notte dall'app. `arg` = cartella di un progetto, `text` = prompt; per remove `arg` = id. */
    @SerialName("night_add") NIGHT_ADD, @SerialName("night_remove") NIGHT_REMOVE,
    /** Contratto 1.19 (R5): «Condividi». `session` = nome, `text` = messaggio, `arg` = id di /share o null (solo testo). */
    @SerialName("report") REPORT,
    /** Contratto 1.21: il tasto Stop, un solo Esc e solo a turno in corso. Si mostra solo se `state.ops` lo contiene. */
    @SerialName("interrupt") INTERRUPT,
    /** Contratto 1.22: la conversazione della sessione a pagine; `arg` = "n", "n:before=<id>" o "n:after=<id>". */
    @SerialName("transcript") TRANSCRIPT,
}

@Serializable data class Option(val n: Int, val label: String)

@Serializable data class Question(
    val id: String, val kind: QuestionKind, val text: String, val options: List<Option>,
    val tier: Tier, @SerialName("asked_at") val askedAt: Long,
)

@Serializable data class Outcome(val short: String, val full: String, val at: Long)

@Serializable data class Session(
    val id: String, val name: String, val account: String, val project: String,
    val state: SessionState, val since: Long,
    @SerialName("turn_started") val turnStarted: Long? = null,
    val tool: String? = null, val link: String = "",
    /** Contratto 1.5: la description che Claude scrive accanto al comando Bash, null se non c'è. */
    @SerialName("tool_note") val toolNote: String? = null,
    val attached: Boolean = false, val followed: Boolean = false,
    val question: Question? = null, val outcome: Outcome? = null, val next: String? = null,
    /** Contratto 1.2: data della riga di recap che ha prodotto `next` (mezzanotte locale), assente se non c'è. */
    @SerialName("next_at") val nextAt: Long? = null,
    /** Contratto 1.1: colore della sessione «#RRGGBB» per il badge; assente → grigio. */
    val color: String? = null,
    /** Contratto 1.1: emoji del badge come su Telegram (es. 🟠 / 🟧); l'app disegna il badge da account + color. */
    val icon: String? = null,
    /** Contratto 1.8: «personal» o «work»; assente con un relay precedente (si ripiega sul nome «personale»). */
    @SerialName("account_kind") val accountKind: String? = null,
    /**
     * Contratto 1.11: modello, effort e contesto della sessione, letti dalla sua trascrizione. Valgono null quando non
     * si leggono: nessuna trascrizione (una sessione sparita), nessun turno dell'assistente, e per `context` anche un
     * cambio di modello a metà sessione, dove la finestra non è certa e il PC non stima.
     */
    val model: Model? = null,
    val effort: String? = null,
    val context: Int? = null,
    /** Contratto 1.16: «off», «offered» (limite raggiunto, Claude Code propone di continuare lento) o «active» (viva ma lenta oltre il limite); null se non si legge. */
    @SerialName("low_priority") val lowPriority: String? = null,
    /** Contratto 1.16: la condizione di completamento data con /goal, null senza obiettivo. */
    val goal: Goal? = null,
)

/** Contratto 1.16: testo dell'obiettivo (/goal), da quando, e se è soddisfatto quando il PC lo sa dire. */
@Serializable data class Goal(val text: String, val since: Long, val met: Boolean? = null)

/**
 * Il modello come lo scrive Claude Code: `id` completo (il suffisso `[1m]` dice la finestra da 1M) e nome breve.
 * Dal vivo `label` arriva null (visto il 16/09 08:09 su tutte le sessioni): il nome si ricava dall'id con `ModelText`.
 */
@Serializable data class Model(val id: String, val label: String? = null)

@Serializable data class QuotaAccount(
    val h5: Int? = null, val w7: Int? = null,
    @SerialName("reset_w7") val resetW7: Long? = null, val stale: Boolean = false,
    /** Contratto 1.3: quando riparte la finestra di 5 ore (epoch in secondi); `reset_w7` resta il reset settimanale. */
    @SerialName("reset_h5") val resetH5: Long? = null,
    /** Contratto 1.8: «personal» o «work»; assente con un relay precedente. */
    val kind: String? = null,
)

@Serializable data class Project(
    val path: String, val name: String, val account: String,
    /** Contratto 1.13: epoch s della trascrizione più recente della cartella nel suo account, null se non ce n'è. */
    @SerialName("last_used") val lastUsed: Long? = null,
)
/**
 * Contratto 1.17: `items` = i lavori di stanotte nell'ordine di esecuzione, sempre presente (anche vuoto) per un relay
 * che la supporta; null = relay precedente, l'app chiede di aggiornare claude-master.
 */
@Serializable data class Night(val queued: Int = 0, val running: String? = null, val items: List<NightItem>? = null)

/** Un lavoro della notte (1.17): `prompt` su una riga, tagliato a fine parola entro 160; `started` null finché non parte. */
@Serializable data class NightItem(
    val id: String, val dir: String, val name: String, val prompt: String, val added: Long, val started: Long? = null,
)
@Serializable data class RecapItem(val project: String, val done: String, val next: String? = null)
@Serializable data class Recap(val date: String = "", val items: List<RecapItem> = emptyList())

@Serializable data class State(
    val v: Int, val ts: Long, val host: String,
    val sessions: List<Session> = emptyList(),
    val quota: Map<String, QuotaAccount> = emptyMap(),
    val projects: List<Project> = emptyList(),
    val night: Night = Night(), val recap: Recap = Recap(),
    /** Contratto 1.19: presente = il relay accetta «Condividi»; `max_bytes` vale sulla stringa cifrata `enc`. */
    val share: Share? = null,
    /** Contratto 1.12: modelli ed effort che il selettore di una sessione accetta; null con un relay precedente. */
    val choices: Choices? = null,
    /** Contratto 1.21: le op di /cmd che questo relay esegue; null con un relay precedente (vale la lista del contratto). */
    val ops: List<String>? = null,
)

/** Contratto 1.12: l'id del modello è quello completo di `model.id` (col suffisso `[1m]` dove c'è). */
@Serializable data class Choices(val models: List<Model> = emptyList(), val efforts: List<String> = emptyList())

@Serializable data class Share(@SerialName("max_bytes") val maxBytes: Int)

@Serializable data class Event(
    val key: String, val kind: EventKind, val session: String? = null, val account: String? = null,
    val ts: Long, val title: String, val body: String = "", val ref: String? = null,
)

@Serializable data class Cmd(
    val id: String, val op: CmdOp, val session: String? = null, val arg: String? = null,
    val issued: Long, val by: String,
    /**
     * Contratto 1.13: il primo messaggio di un `launch`. Assente, non scritto: un comando senza messaggio resta identico
     * a prima, e un relay vecchio lancia senza messaggio invece di uno a metà.
     */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val text: String? = null,
    /** Contratto 1.22: "phone" o "watch", per il prefisso del prompt e l'`origin` della trascrizione. Assente, non scritto. */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val device: String? = null,
)

@Serializable data class CmdResult(
    val id: String, val ok: Boolean, val text: String, val at: Long,
    /** Contratto 1.13: la sessione nata da un `launch`, come in `sessions[].name`; c'è anche se il messaggio non è arrivato. */
    val session: String? = null,
    /** Contratto 1.17: l'id del lavoro accodato da `night_add`. */
    val job: String? = null,
)

/**
 * Contratto 1.22: una voce della conversazione di una sessione, dalla sua trascrizione. `role` = user, assistant o tool;
 * `id` è opaco e serve solo per le pagine `before`/`after`; `cut` = testo accorciato dal PC oltre 4000 caratteri; `turn`
 * sta sull'ultima voce di un turno chiuso; `files` = i file prodotti in quel passo (percorso sul PC).
 */
@Serializable data class TranscriptEntry(
    val id: String, val role: String, val text: String? = null, val at: Long? = null,
    val tool: String? = null, val note: String? = null, val error: Boolean? = null, val cut: Boolean = false,
    val turn: TranscriptTurn? = null, val files: List<TranscriptFile>? = null,
    /** Da dove è arrivato un messaggio dell'utente (phone, watch, pc); null con un relay che non lo dice. */
    val origin: String? = null,
)

/** `in` comprende la cache letta e scritta; per il costo del turno conta di più `out`. */
@Serializable data class TranscriptTurn(val started: Long? = null, val ended: Long? = null, @SerialName("in") val input: Long? = null, val out: Long? = null)

@Serializable data class TranscriptFile(val path: String, val mime: String? = null, val size: Long? = null)

@Serializable data class TranscriptPage(val entries: List<TranscriptEntry> = emptyList(), val more: Boolean = false)
