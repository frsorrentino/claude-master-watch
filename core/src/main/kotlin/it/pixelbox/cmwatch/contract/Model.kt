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
    @SerialName("gone") GONE, @SerialName("launched") LAUNCHED, @SerialName("quota") QUOTA, @SerialName("resumed") RESUMED
}

@Serializable
enum class CmdOp {
    @SerialName("answer") ANSWER, @SerialName("prompt") PROMPT, @SerialName("launch") LAUNCH,
    @SerialName("follow") FOLLOW, @SerialName("unfollow") UNFOLLOW, @SerialName("resume") RESUME,
    @SerialName("screen") SCREEN, @SerialName("allow_all") ALLOW_ALL
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
    val attached: Boolean = false, val followed: Boolean = false,
    val question: Question? = null, val outcome: Outcome? = null, val next: String? = null,
    /** Contratto 1.2: data della riga di recap che ha prodotto `next` (mezzanotte locale), assente se non c'è. */
    @SerialName("next_at") val nextAt: Long? = null,
    /** Contratto 1.1: colore della sessione «#RRGGBB» per il badge; assente → grigio. */
    val color: String? = null,
    /** Contratto 1.1: emoji del badge come su Telegram (es. 🟠 / 🟧); l'app disegna il badge da account + color. */
    val icon: String? = null,
)

@Serializable data class QuotaAccount(
    val h5: Int? = null, val w7: Int? = null,
    @SerialName("reset_w7") val resetW7: Long? = null, val stale: Boolean = false,
)

@Serializable data class Project(val path: String, val name: String, val account: String)
@Serializable data class Night(val queued: Int = 0, val running: String? = null)
@Serializable data class RecapItem(val project: String, val done: String, val next: String? = null)
@Serializable data class Recap(val date: String = "", val items: List<RecapItem> = emptyList())

@Serializable data class State(
    val v: Int, val ts: Long, val host: String,
    val sessions: List<Session> = emptyList(),
    val quota: Map<String, QuotaAccount> = emptyMap(),
    val projects: List<Project> = emptyList(),
    val night: Night = Night(), val recap: Recap = Recap(),
)

@Serializable data class Event(
    val key: String, val kind: EventKind, val session: String? = null, val account: String? = null,
    val ts: Long, val title: String, val body: String = "", val ref: String? = null,
)

@Serializable data class Cmd(
    val id: String, val op: CmdOp, val session: String? = null, val arg: String? = null,
    val issued: Long, val by: String,
)

@Serializable data class CmdResult(val id: String, val ok: Boolean, val text: String, val at: Long)
