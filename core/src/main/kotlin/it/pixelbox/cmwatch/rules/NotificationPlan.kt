package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.contract.Tier

/**
 * Piano di una notifica (specifica di Franz, 12/09 15:25 e 15:26): tutto ciò che il Notifier di :wear deve costruire,
 * senza Android. Testabile: titolo, righe della conversazione, azioni per tier, chip, cronometro, canale, gruppo.
 */
object NotificationPlan {
    const val CH_QUESTIONS = "domande"
    const val CH_OUTCOMES = "esiti"
    const val CH_GONE = "sparite"
    const val CH_QUOTA = "quota"
    const val GROUP = "cm"

    data class Labels(
        val open: String, val reply: String, val retry: String, val read: String, val stop: String, val write: String, val resume: String,
        val sent: String, val confirmed: String, val notDelivered: String, val sessions: String,
    )

    sealed class Act {
        data class Option(val n: Int, val label: String) : Act()
        data object Reply : Act()
        data object Open : Act()
        data object Retry : Act()
        data object Read : Act()
        data object Write : Act()
        data object Resume : Act()
    }

    /** Una domanda già risposta, che resta nel thread. */
    data class Qa(val question: String, val answer: String, val at: Long)

    data class Plan(
        val session: String?, val title: String, val person: String?, val messages: List<String>, val bigText: String?,
        val actions: List<Act>, val choices: List<String>, val freeForm: Boolean, val channel: String,
        val whenS: Long?, val chronometer: Boolean, val subText: String?, val autoCancel: Boolean,
        val timeoutMs: Long?, val progress: Int?, val accent: SessionState?,
    )

    data class Summary(val title: String, val rows: List<String>)

    private fun dot(account: String) = if (account == "agenzia") "🔴" else "🟢"
    private fun optionLabel(n: Int, label: String) = "$n $label"

    fun question(s: Session, l: Labels, history: List<Qa>): Plan {
        val q = s.question ?: error("no question")
        val high = q.tier == Tier.HIGH
        val actions = buildList {
            if (!high) { q.options.take(2).forEach { add(Act.Option(it.n, optionLabel(it.n, it.label))) }; add(Act.Reply) }
            add(Act.Open)
        }
        return Plan(
            session = s.name, title = "❓ ${s.name}", person = "${dot(s.account)} ${s.name}",
            messages = history.map { "❓ ${it.question} → ${it.answer}" } + q.text, bigText = null,
            actions = actions, choices = if (high) emptyList() else q.options.map { optionLabel(it.n, it.label) },
            freeForm = !high, channel = CH_QUESTIONS, whenS = q.askedAt, chronometer = true, subText = s.account,
            autoCancel = false, timeoutMs = null, progress = null, accent = SessionState.WAITING,
        )
    }

    /**
     * Il tasto di lettura della notifica: «Leggi», e «Ferma» mentre la voce legge quello che ha fatto partire (Franz,
     * 14/09 16:08: «rimane leggi mentre è in riproduzione»). Un secondo tocco ferma già; così si vede.
     */
    fun readLabel(reading: Boolean, l: Labels) = if (reading) l.stop else l.read

    fun sentLine(n: Int, label: String, l: Labels) = "✓ $n · $label ${l.sent}"
    fun confirmedLine(l: Labels) = "✓ ${l.confirmed}"
    fun failedLine(l: Labels) = "✗ ${l.notDelivered} · ${l.retry}"

    fun outcome(s: Session, l: Labels): Plan {
        val o = s.outcome ?: error("no outcome")
        return Plan(
            session = s.name, title = "✓ ${s.name}", person = null, messages = listOf(o.short), bigText = o.full,
            actions = listOf(Act.Read, Act.Write, Act.Open), choices = emptyList(), freeForm = true, channel = CH_OUTCOMES,
            whenS = o.at, chronometer = false, subText = s.account, autoCancel = true, timeoutMs = 12 * 3600_000L,
            progress = null, accent = SessionState.IDLE,
        )
    }

    fun gone(name: String, account: String?, l: Labels): Plan = Plan(
        session = name, title = "✗ $name", person = null, messages = emptyList(), bigText = null,
        actions = listOf(Act.Resume), choices = emptyList(), freeForm = false, channel = CH_GONE,
        whenS = null, chronometer = false, subText = account, autoCancel = true, timeoutMs = null, progress = null, accent = SessionState.GONE,
    )

    fun quota(account: String, q: QuotaAccount, l: Labels, threshold: Int = 95): Plan {
        val pct = maxOf(q.h5 ?: 0, q.w7 ?: 0)
        return Plan(
            session = null, title = "⚠ $pct % $account", person = null, messages = emptyList(), bigText = null,
            actions = listOf(Act.Open), choices = emptyList(), freeForm = false, channel = CH_QUOTA,
            whenS = null, chronometer = false, subText = null, autoCancel = true, timeoutMs = null, progress = pct, accent = null,
        )
    }

    /** Summary del gruppo: una riga per sessione con lo stato. */
    fun summary(state: State, l: Labels): Summary {
        val q = state.sessions.count { it.question != null }
        val title = "${state.sessions.size} ${l.sessions}" + if (q > 0) " · $q?" else ""
        val rows = state.sessions.map { s ->
            val icon = if (s.question != null) "❓" else when (s.state) {
                SessionState.WAITING -> "❓"; SessionState.BUSY, SessionState.AWAITING -> "▶"; SessionState.IDLE -> "✓"; SessionState.GONE -> "✗"
            }
            "$icon ${s.name}"
        }
        return Summary(title, rows)
    }
}
