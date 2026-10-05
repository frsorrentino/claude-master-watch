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
    /**
     * Contratto 1.24: un file della conversazione; `arg` = il `path` di `transcript.entries[].files[]`. Il relay scrive
     * `/file/<id del comando>` (busta come `/share`, in chiaro `{mime, data}`), il dispositivo lo legge e lo cancella.
     */
    @SerialName("file") FILE,
    /**
     * Contratto 1.25: un comando slash digitato nel pannello della sessione senza prefisso; `arg` = il comando senza «/»,
     * `text` = gli argomenti o null. Mai a metà turno («<name> is busy»).
     */
    @SerialName("slash") SLASH,
    /**
     * Contratto 1.26: tutti i progetti di tutti gli account per «Lancia», fuori da /state (che li taglia a 5 per stare in
     * 8 KB). `session` e `arg` null; il testo è un `ProjectsPage`. Una lettura: nessuna push dopo.
     */
    @SerialName("projects") PROJECTS,
    /**
     * Contratto 1.27: cerca nelle conversazioni di tutte le sessioni (vive e chiuse degli ultimi 7 giorni). `session`
     * null, `arg` il testo (1-200 caratteri); il testo del risultato è un `SearchPage`; rifiuto «bad query: …».
     */
    @SerialName("search") SEARCH,
    /**
     * Contratto 1.29: cosa hanno fatto le sessioni, in ordine di tempo. `session` = un nome o null per tutte, `arg` =
     * «90m», «6h», «2d» (al massimo 7 giorni) o un epoch, null = 6h; il testo del risultato è un `TimelinePage`.
     */
    @SerialName("timeline") TIMELINE,
    /**
     * Contratto 1.31: accoppiare un dispositivo nuovo dal telefono, senza PC. `session` e `arg` null; il relay apre da sé
     * `relay pair --add` per 5 minuti e risponde con un `PairAddOffer`. Una lettura: nessuna push dopo.
     */
    @SerialName("pair_add") PAIR_ADD,
    /** Contratto 1.37: l'ok a un compito che lo aspetta; `arg` = id del compito, `text` facoltativo («ok»). */
    @SerialName("approve") APPROVE,
    /** Contratto 1.37: una decisione da salvare nella memoria della master; `text` obbligatorio, `arg` = progetto o null. */
    @SerialName("decision") DECISION,
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
    /** Contratto 1.23: il prompt suggerito che il terminale mostra attenuato dopo ❯; solo per una sessione ferma. */
    val suggestion: String? = null,
    /** Contratto 1.37: il consiglio di fable-director; assente quando non c'è. */
    val advice: Advice? = null,
    /** Contratto 1.37: ferma, con «Esito:» e senza «Prossimi:»; assente = false. */
    val finished: Boolean = false,
    /** Contratto 1.37: la sessione aperta prima sulla stessa cartella e conversazione; assente = null. */
    @SerialName("duplicate_of") val duplicateOf: String? = null,
)

/** Contratto 1.37: `when` "now" o "next_task"; `differs` = la scelta attuale è diversa (il puntino sul tasto). */
@Serializable data class Advice(
    val model: String, val effort: String, val reason: String,
    @SerialName("switch_cost_tokens") val switchCostTokens: Long = 0, val at: Long = 0, val source: String? = null,
    @SerialName("when") val whenToSwitch: String? = null, val differs: Boolean = false,
)

/** Contratto 1.37: un compito del registro che aspetta l'ok; `deploy` = esce in produzione. */
@Serializable data class Approval(
    val task: String, val title: String, val what: String? = null, val where: String? = null, val deploy: Boolean = false,
    @SerialName("requested_at") val requestedAt: Long = 0,
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
    /** Contratto 1.25: i comandi slash che il relay consente (senza «/»); null con un relay precedente. */
    val slash: List<String>? = null,
    /** Contratto 1.32: i dispositivi accoppiati, nell'ordine di accoppiamento; null con un relay precedente. */
    val devices: List<Device>? = null,
    /** Contratto 1.33: le azioni ricorrenti della master, dall'ultima usata in cima, al massimo 8; null senza lista. */
    val recurring: List<Recurring>? = null,
    /** Contratto 1.37: i compiti che aspettano l'ok, dal più vecchio; vuota con un relay precedente. */
    val approvals: List<Approval> = emptyList(),
)

/**
 * Contratto 1.32: un dispositivo accoppiato come lo vede il PC. `kind` = phone, watch, tablet, chromebook o null se non si
 * sa; `seen` = epoch s dell'ultima lettura (`/seen`), null se non è mai arrivata.
 */
@Serializable data class Device(val uid: String, val name: String, val kind: String? = null, val seen: Long? = null)

/**
 * Contratto 1.33: un'azione ricorrente della master (Franz, 04/10 20:24). `param` = il prompt aspetta un pezzo da
 * aggiungere (un link, un numero di release): va nel campo col cursore in fondo, senza invio diretto.
 */
@Serializable data class Recurring(val id: String, val label: String, val prompt: String, val param: Boolean = false)

/** Contratto 1.12: l'id del modello è quello completo di `model.id` (col suffisso `[1m]` dove c'è). */
@Serializable data class Choices(val models: List<Model> = emptyList(), val efforts: List<String> = emptyList())

/**
 * Contratto 1.19 «Condividi»; 1.28: `any` = il relay accetta file di qualunque formato (non solo JPEG e PNG), con `name`
 * nel blob di /share; false con un relay precedente.
 */
@Serializable data class Share(@SerialName("max_bytes") val maxBytes: Int, val any: Boolean = false)

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
    /** Contratto 1.34: `file` a pezzi (/file/<id>/parts e /meta). Assente, non scritto: un relay vecchio scrive il nodo unico. */
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val parts: Boolean? = null,
)

/**
 * Contratto 1.34: il manifesto di un file a pezzi, in chiaro dentro /file/<id>/meta, scritto dal relay dopo tutti i pezzi.
 * `n` pezzi da 1 MB (l'ultimo più corto), `size` e `sha256` (esadecimale) del file intero.
 */
@Serializable data class FileMeta(val n: Int, val size: Long, val sha256: String, val mime: String, val name: String? = null)

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
    /** Scritto a turno in corso e non ancora preso da Claude: «in coda». Poi la stessa voce (stesso id) diventa false. */
    val queued: Boolean = false,
)

/** `in` comprende la cache letta e scritta; per il costo del turno conta di più `out`. */
@Serializable data class TranscriptTurn(val started: Long? = null, val ended: Long? = null, @SerialName("in") val input: Long? = null, val out: Long? = null)

@Serializable data class TranscriptFile(val path: String, val mime: String? = null, val size: Long? = null)

/** Contratto 1.26: il risultato di `projects`, dal più recente (`last_used` null in fondo); `more` oltre i 60 KB. */
@Serializable data class ProjectsPage(val projects: List<Project> = emptyList(), val more: Boolean = false)

/**
 * Contratto 1.27: un punto trovato. `session` è il nome nel contratto se viva, se no quello della cartella; `entry` l'id
 * di `transcript`; `match` = [inizio, fine) della parola trovata dentro `snippet` (fino a 160 caratteri, senza «…»).
 */
@Serializable data class SearchHit(
    val session: String, val live: Boolean, val project: String? = null, val entry: String? = null,
    val role: String? = null, val at: Long? = null, val snippet: String, val match: List<Int> = emptyList(),
)

@Serializable data class SearchPage(val hits: List<SearchHit> = emptyList(), val more: Boolean = false)

/**
 * Contratto 1.29: un evento della cronologia, sempre con tutte le chiavi. `kind`: prompt (`ref` = phone o watch se arriva
 * dal relay, null se scritto al PC), test (`text` = la suite, `ok` verde o rosso, `ref` = la riga di riepilogo), commit
 * (`ref` = hash corto), outcome (la riga «Esito:»), task (`ok` dal controllo rieseguito, `ref` = id del compito). `text`
 * al massimo 160 caratteri su una riga. Stringa, non enum: un kind nuovo del relay non rompe la lettura.
 */
@Serializable data class TimelineEvent(val at: Long, val kind: String, val text: String, val ok: Boolean? = null, val ref: String? = null)

/** Una sessione della cronologia: `project` relativo come `state.sessions[].project`; `live` distingue la stessa cartella viva e chiusa. */
@Serializable data class TimelineSession(val session: String, val live: Boolean, val project: String? = null, val events: List<TimelineEvent> = emptyList())

/** Contratto 1.29: le sessioni dalla più recente, al massimo 40 eventi ciascuna; `more` = tagliata oltre 60 KB. */
@Serializable data class TimelinePage(val since: Long, val sessions: List<TimelineSession> = emptyList(), val more: Boolean = false)

/**
 * Contratto 1.31: l'invito aperto dal relay per un dispositivo in più. `qr` = il documento del QR della 1.30 (con `m`
 * «add»), null se mancano i dati dell'app Firebase; `code` = il codice a 6 cifre; `exp` = quando scade (epoch s).
 */
@Serializable data class PairAddOffer(val qr: kotlinx.serialization.json.JsonObject? = null, val code: String, val exp: Long)

/** L'invito per chi lo mostra: il QR già come testo da disegnare (lo stesso che `PairQr.parse` legge). */
data class PairAddInvite(val qr: String?, val code: String, val exp: Long)

@Serializable data class TranscriptPage(val entries: List<TranscriptEntry> = emptyList(), val more: Boolean = false)
