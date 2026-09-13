package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Durations
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State

/** Testi della tile (design, sezione 2): conteggi, la sessione ferma su una riga intera, bottoni, freschezza adattiva. */
object TileTexts {
    enum class Button { OPEN, SESSIONS, QUOTA, REPLY }
    enum class Accent { QUESTION, BUSY, IDLE, STALE }
    data class Labels(val none: String, val stale: String, val works: String)

    /** Cosa mostra la tile a colpo d'occhio: etichetta dei conteggi, sessione in evidenza, corpo, un solo bottone di bordo. */
    data class Glance(val counts: String, val name: String?, val body: String, val button: Button, val accent: Accent, val target: String)

    /** Contatori della tile: solo quelli non a zero, al massimo tre (Franz via la master, 13/09 10:50). */
    data class Card(val kind: Kind, val count: Int) { enum class Kind { ACTIVE, IDLE, GONE } }

    fun cards(state: State, seen: Set<String>): List<Card> {
        val active = state.sessions.count { it.state == SessionState.BUSY || it.state == SessionState.AWAITING || it.state == SessionState.WAITING }
        val idle = state.sessions.count { it.state == SessionState.IDLE }
        val gone = state.sessions.count { it.state == SessionState.GONE }
        return listOf(Card(Card.Kind.ACTIVE, active), Card(Card.Kind.IDLE, idle), Card(Card.Kind.GONE, gone)).filter { it.count > 0 }.take(3)
    }

    /** Etichetta del bottone di bordo, che porta anche il conteggio (come «2 new» della tile di Gmail). */
    data class Edge(val kind: Kind, val count: Int) { enum class Kind { REPLY, QUESTIONS, ACTIVE, SESSIONS } }

    fun edge(state: State?, freshness: Freshness, seen: Set<String>): Edge {
        if (state == null || freshness is Freshness.Stale) return Edge(Edge.Kind.SESSIONS, 0)
        val questions = state.sessions.count { s -> s.question?.let { it.id !in seen } == true }
        if (questions == 1) return Edge(Edge.Kind.REPLY, 1)
        if (questions > 1) return Edge(Edge.Kind.QUESTIONS, questions)
        // «attive» ha lo stesso significato delle card: né ferme né sparite (una domanda già vista resta attiva).
        val active = state.sessions.count { it.state == SessionState.BUSY || it.state == SessionState.AWAITING || it.state == SessionState.WAITING }
        return if (active > 0) Edge(Edge.Kind.ACTIVE, active) else Edge(Edge.Kind.SESSIONS, 0)
    }

    /**
     * Cosa mette la tile nella card quando non c'è una domanda. Se nessuna sessione lavora e l'ultimo movimento è
     * più vecchio di un quarto d'ora, l'esito vecchio non è una notizia: si dice lo stato vero (Franz, 13/09 17:55:
     * «così com'è mi sembra inutile e datato»).
     */
    sealed interface Rest {
        /** Una sessione da mostrare:  = sta lavorando ora, altrimenti è l'ultima che si è mossa. */
        data class Live(val session: Session, val busy: Boolean) : Rest
        /** Nessun movimento recente: quante sessioni ci sono e da quanto tutto è fermo. */
        data class Calm(val sessions: Int, val since: Long?) : Rest
    }

    const val REST_FRESH_S = 15 * 60L

    /**
     * Cosa scrivere nella card della sessione. Prima l'attività di adesso (`tool`), poi la cosa più fresca fra
     * l'esito e il prossimo passo: `next` non ha una data nel contratto e resta indietro, quindi la card sembrava
     * congelata con lo stesso testo mentre scorreva solo il minuto (Franz, 13/09 18:23).
     * Il testo si taglia alla prima frase, così non finisce dentro una parentesi aperta.
     */
    fun activity(s: Session, busy: Boolean, running: String, idle: String): String {
        val tool = s.tool?.trim()?.takeIf { it.isNotEmpty() }
        val esito = s.outcome?.short?.trim()?.takeIf { it.isNotEmpty() }
        val prossimo = s.next?.trim()?.takeIf { it.isNotEmpty() }
        val esitoPiuFresco = (s.outcome?.at ?: 0L) >= (s.turnStarted ?: 0L)
        val scelto = when {
            busy && tool != null -> tool
            busy && esito != null && esitoPiuFresco -> esito
            busy -> prossimo ?: running
            else -> esito ?: prossimo ?: idle
        }
        return primaFrase(scelto)
    }

    /** Prima frase, se finisce entro una riga e mezza: meglio un pensiero intero che una coda troncata. */
    fun primaFrase(text: String, max: Int = 46): String {
        val punto = text.indexOf(". ")
        return if (punto in 1..max) text.substring(0, punto + 1) else text
    }


    private fun moved(s: Session): Long = maxOf(s.since, s.turnStarted ?: 0L, s.outcome?.at ?: 0L)

    fun rest(state: State, now: Long): Rest {
        val live = state.sessions.filter { it.state != SessionState.GONE }
        val working = live.firstOrNull { it.followed && (it.state == SessionState.BUSY || it.state == SessionState.AWAITING) }
            ?: live.firstOrNull { it.state == SessionState.BUSY || it.state == SessionState.AWAITING || it.state == SessionState.WAITING }
        if (working != null) return Rest.Live(working, busy = true)
        val recente = live.maxByOrNull { moved(it) }
        val quando = recente?.let { moved(it) }
        if (recente != null && quando != null && now - quando <= REST_FRESH_S) return Rest.Live(recente, busy = false)
        return Rest.Calm(sessions = live.size, since = quando)
    }

    /** Badge della tile: l'emoji del contratto 1.1, altrimenti il glifo dello stato (la tile non disegna, scrive). */
    fun badge(s: Session): String = s.icon?.takeIf { it.isNotBlank() } ?: if (s.question != null) "❓" else icon(s.state)

    /** «ferma da 5 m» dall'istante della domanda. */
    fun waitingFor(s: Session, now: Long, pattern: String): String =
        pattern.format(Durations.since(s.question?.askedAt ?: s.since, now))

    fun counts(state: State): String {
        val q = state.sessions.count { it.question != null }
        val busy = state.sessions.count { it.question == null && (it.state == SessionState.BUSY || it.state == SessionState.AWAITING) }
        val idle = state.sessions.count { it.state == SessionState.IDLE }
        val gone = state.sessions.count { it.state == SessionState.GONE }
        return listOf(q to "❓", busy to "▶", idle to "✓", gone to "✗").filter { it.first > 0 }.joinToString(" · ") { "${it.first} ${it.second}" }
    }

    fun glance(state: State?, freshness: Freshness, now: Long, l: Labels, seen: Set<String> = emptySet()): Glance {
        if (state == null) return Glance("", null, l.none, Button.SESSIONS, Accent.IDLE, "cmwatch://sessions")
        if (freshness is Freshness.Stale) return Glance("", null, staleLine(freshness, l.stale), Button.SESSIONS, Accent.STALE, "cmwatch://sessions")
        val counts = counts(state)
        state.sessions.firstOrNull { it.question != null && it.question.id !in seen }?.let {
            return Glance(counts, it.name, it.question!!.text, Button.REPLY, Accent.QUESTION, "cmwatch://question/${it.name}")
        }
        val s = state.sessions.firstOrNull { it.followed && it.state != SessionState.GONE }
            ?: state.sessions.filter { it.state != SessionState.GONE }.maxByOrNull { maxOf(it.since, it.turnStarted ?: 0, it.outcome?.at ?: 0) }
            ?: return Glance(counts, null, l.none, Button.SESSIONS, Accent.IDLE, "cmwatch://sessions")
        val busy = s.state == SessionState.BUSY || s.state == SessionState.AWAITING
        val age = Durations.since(if (busy) s.turnStarted ?: s.since else s.since, now)
        val body = if (busy) "▶ ${s.tool ?: l.works} · $age" else s.outcome?.short ?: "✓ · $age"
        return Glance(counts, s.name, body, Button.SESSIONS, if (busy) Accent.BUSY else Accent.IDLE, "cmwatch://session/${s.name}")
    }

    fun header(state: State, sessionsLabel: String): String {
        val q = state.sessions.count { it.question != null }
        val g = state.sessions.count { it.state == SessionState.GONE }
        val parts = mutableListOf("${state.sessions.size} $sessionsLabel")
        if (q > 0) parts += "$q?"
        if (g > 0) parts += "$g✗"
        return parts.joinToString(" · ")
    }

    private fun icon(s: SessionState) = when (s) {
        SessionState.WAITING -> "❓"; SessionState.BUSY, SessionState.AWAITING -> "▶"; SessionState.IDLE -> "✓"; SessionState.GONE -> "✗"
    }

    /** La sessione ferma con la domanda intera; senza domande la seguita, altrimenti la più recente. */
    fun line(state: State, now: Long): String? {
        state.sessions.firstOrNull { it.question != null }?.let { return "❓ ${it.name} · ${it.question!!.text}" }
        val s = state.sessions.firstOrNull { it.followed && it.state != SessionState.GONE }
            ?: state.sessions.filter { it.state != SessionState.GONE }.maxByOrNull { maxOf(it.since, it.turnStarted ?: 0, it.outcome?.at ?: 0) }
            ?: return null
        return "${icon(s.state)} ${SessionsText.row(s, now)}"
    }

    fun buttons(state: State): List<Button> =
        if (state.sessions.any { it.question != null }) listOf(Button.OPEN, Button.SESSIONS) else listOf(Button.SESSIONS, Button.QUOTA)

    /** 30 s con domande aperte, 15 min altrimenti. */
    fun freshnessMs(state: State): Long = if (state.sessions.any { it.question != null }) 30_000L else 15 * 60_000L

    fun staleLine(f: Freshness.Stale, pattern: String): String = pattern.format(f.minutes)
}
