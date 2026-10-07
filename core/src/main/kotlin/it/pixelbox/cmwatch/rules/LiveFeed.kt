package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Approval
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.EventKind
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Il notiziario della modalità live (specifica live, §5): dallo stato precedente, da quello nuovo e dagli eventi, la coda
 * delle notizie da leggere in cuffia. Come `Wake.plan`, funzioni pure: la coda (`Feed`) la tiene il servizio e passa da
 * qui a ogni stato nuovo e a ogni tocco; l'ora entra da fuori, in secondi epoch come nel contratto.
 *
 * Livelli: 1 serve Franz (domanda, richiesta di ok, Prossimo con «!», riavvio fallito, quota oltre la soglia), senza
 * limiti; 2 esiti e chiusure, al massimo uno ogni 3 minuti per sessione; 3 informazioni (`launched`, `recap`, le notizie
 * di routine delle sessioni in bassa priorità attiva), che non entrano in coda. Domanda, esito e chiusura arrivano dallo
 * stato, che ha sempre il più recente; degli eventi contano `answered` e `restart_failed`.
 */
object LiveFeed {
    /** Per ogni sessione, al massimo una notizia di livello 2 ogni 3 minuti. */
    const val GAP_S = 180L

    /** «Dopo» rimette la notizia in coda fra 5 minuti. */
    const val LATER_S = 300L

    /** La quota da cui serve Franz: la stessa soglia delle notifiche (`Wake.plan`) e dell'avviso del relay. */
    const val QUOTA_HOT = 95

    /** Una domanda più lunga si legge fino all'ultima frase intera entro questo limite, poi «continua sul watch». */
    const val QUESTION_MAX = 300

    /** Le informazioni (livello 3) per il giro completo: al massimo 5, dalle ultime 2 ore. */
    const val INFO_MAX = 5
    const val INFO_KEEP_S = 7_200L

    private const val LOW_ACTIVE = "active"
    private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
    private val FINE_FRASE = Regex("[.!?](?=\\s)")

    enum class Kind { QUESTION, APPROVAL, OUTCOME, GONE, RESTART_FAILED, QUOTA, LAUNCHED, RECAP, MASTER }

    /**
     * Una notizia pronta per la voce. `key`: `s:<sessione>` per domanda, esito e chiusura (una notizia per sessione, con
     * lo stato più recente), `ok:<compito>`, `quota:<account>`, `restart:<sessione>`. `notBefore` = «Dopo».
     */
    data class News(
        val key: String, val level: Int, val kind: Kind, val session: String?, val text: String,
        val questionId: String? = null, val at: Long, val notBefore: Long = 0,
    )

    /**
     * La coda e, per sessione, quando si è letta la sua ultima notizia di livello 2 (l'attesa di 3 minuti). `info` = le
     * notizie di livello 3 (`launched`, `recap`), che non entrano in coda: le legge il giro completo.
     */
    data class Feed(
        val items: List<News> = emptyList(), val spokenAt: Map<String, Long> = emptyMap(), val info: List<News> = emptyList(),
    )

    /**
     * Le frasi, da strings.xml. `numbers` = i numeri in parole dall'uno («uno», «due»); `code` = cosa dice la voce al
     * posto di un blocco di codice (`SpeechText.clean`).
     */
    data class Labels(
        val code: String, val numbers: List<String>, val question: String, val option: String, val more: String,
        val approval: String, val where: String, val outcome: String, val next: String, val step: String,
        val gone: String, val restartFailed: String, val quota: String, val quotaNoReset: String,
        val busy: String, val idle: String, val launched: String, val recap: String,
    )

    /**
     * La coda dopo uno stato nuovo. `events` = quelli arrivati dall'ultima chiamata; `prev` null = primo stato della
     * live, dove si annuncia solo il livello 1 aperto (gli esiti vecchi non sono notizie).
     */
    fun plan(feed: Feed, prev: State?, cur: State, events: List<Event>, now: Long, l: Labels, zone: ZoneId): Feed {
        val before = prev?.sessions?.associateBy { it.name }.orEmpty()
        val items = feed.items.toMutableList()
        fun put(n: News) {
            val i = items.indexOfFirst { it.key == n.key }
            if (i >= 0) items[i] = n else items += n
        }
        for (s in cur.sessions) sessionNews(s, before[s.name], prev == null, now, l)?.let(::put)
        val names = cur.sessions.map { it.name }.toSet()
        for (p in before.values) if (p.name !in names && p.state != SessionState.GONE && !low(p)) put(goneNews(p.name, now, l))
        for (a in cur.approvals) if (prev?.approvals?.none { it.task == a.task } != false) {
            put(News("ok:${a.task}", 1, Kind.APPROVAL, null, approvalText(a, l), at = now))
        }
        for ((account, q) in cur.quota) if (hot(q) && prev?.quota?.get(account)?.let(::hot) != true) {
            put(News("quota:$account", 1, Kind.QUOTA, null, quotaText(account, q, l, zone), at = now))
        }
        for (e in events) if (e.kind == EventKind.RESTART_FAILED) e.ref?.let { name ->
            put(News("restart:$name", 1, Kind.RESTART_FAILED, name, l.restartFailed.format(SpeakableName.of(name)), at = now))
        }
        // Via le notizie superate: domanda risposta (anche prima che lo stato lo dica) o sparita, ok sparito, quota scesa.
        val answered = events.filter { it.kind == EventKind.ANSWERED }.mapNotNull { it.ref }.toSet()
        val byName = cur.sessions.associateBy { it.name }
        val tasks = cur.approvals.map { it.task }.toSet()
        return feed.copy(info = info(feed.info, events, now, l), items = items.filterNot { n ->
            when (n.kind) {
                Kind.QUESTION -> n.questionId in answered || n.session?.let { byName[it] }?.question?.id != n.questionId
                Kind.APPROVAL -> n.key.removePrefix("ok:") !in tasks
                Kind.QUOTA -> cur.quota[n.key.removePrefix("quota:")]?.let(::hot) != true
                else -> false
            }
        })
    }

    /**
     * La prossima notizia da leggere: il livello 1 prima, poi la più vecchia. Il servizio la chiede solo a frase finita:
     * il livello 1 passa davanti, ma non interrompe. `onlyBlocking` = la modalità «solo bloccanti».
     */
    fun next(feed: Feed, now: Long, onlyBlocking: Boolean = false): News? = feed.items
        .filter { n ->
            val last = n.session?.let { feed.spokenAt[it] }
            n.notBefore <= now && (!onlyBlocking || n.level == 1) && (n.level == 1 || last == null || now >= last + GAP_S)
        }
        .minWithOrNull(compareBy<News> { it.level }.thenBy { it.at })

    /** Notizia letta: esce dalla coda; se è di livello 2, da qui partono i 3 minuti della sua sessione. «Ripeti» non passa di qui. */
    fun spoken(feed: Feed, key: String, now: Long): Feed {
        val n = feed.items.firstOrNull { it.key == key } ?: return feed
        val at = if (n.level >= 2 && n.session != null) feed.spokenAt + (n.session to now) else feed.spokenAt
        return Feed(feed.items - n, at)
    }

    /** «Dopo»: la stessa notizia torna fra 5 minuti; un aggiornamento della sessione nel frattempo la sostituisce. */
    fun later(feed: Feed, key: String, now: Long): Feed =
        feed.copy(items = feed.items.map { if (it.key == key) it.copy(notBefore = now + LATER_S) else it })

    /** «Salta»: la notizia esce dalla coda e nelle sessioni non cambia nulla. */
    fun skip(feed: Feed, key: String): Feed = feed.copy(items = feed.items.filterNot { it.key == key })

    /**
     * Il giro completo, una frase per sessione: prima quelle che aspettano Franz (domanda o Prossimo con «!») e le
     * richieste di ok, poi quelle al lavoro, poi quelle seguite e ferme. Le sessioni in bassa priorità attiva restano
     * fuori, salvo quando aspettano Franz: il livello 1 non ha limiti.
     */
    fun round(state: State, l: Labels, feed: Feed = Feed()): List<String> {
        val open = state.sessions.filter { it.state != SessionState.GONE }
        val waiting = open.filter { it.question != null || blocking(it) }
        val working = open.filter { it !in waiting && it.state == SessionState.BUSY && !low(it) }
        val stopped = open.filter { it !in waiting && it.state != SessionState.BUSY && it.followed && !low(it) }
        return waiting.map { if (it.question != null) questionText(it, l) else outcomeText(it, l) } +
            state.approvals.map { approvalText(it, l) } +
            working.map { l.busy.format(SpeakableName.of(it.name)) } +
            stopped.map { if (it.outcome != null) outcomeText(it, l) else l.idle.format(SpeakableName.of(it.name)) } +
            feed.info.map { it.text }
    }

    /** Il giro completo è stato letto: le informazioni non si ripetono al giro dopo. */
    fun heard(feed: Feed): Feed = feed.copy(info = emptyList())

    /** «com'è messa nome»: la domanda aperta, l'esito, al lavoro, chiusa o ferma. */
    fun status(s: Session, l: Labels): String = when {
        s.question != null -> questionText(s, l)
        s.state == SessionState.GONE -> l.gone.format(SpeakableName.of(s.name))
        s.state == SessionState.BUSY && !blocking(s) -> l.busy.format(SpeakableName.of(s.name))
        s.outcome != null -> outcomeText(s, l)
        else -> l.idle.format(SpeakableName.of(s.name))
    }

    /** «chi mi aspetta»: le domande, i Prossimi con «!» e le richieste di ok; vuota se nessuno aspetta. */
    fun waiting(state: State, l: Labels): List<String> {
        val open = state.sessions.filter { it.state != SessionState.GONE && (it.question != null || blocking(it)) }
        return open.map { if (it.question != null) questionText(it, l) else outcomeText(it, l) } + state.approvals.map { approvalText(it, l) }
    }

    /** «quanta quota»: una frase per account, nell'ordine dello stato. */
    fun quotaAll(state: State, l: Labels, zone: ZoneId): List<String> = state.quota.map { (account, q) -> quotaText(account, q, l, zone) }

    /** Le informazioni nuove dagli eventi, senza doppioni, al massimo [INFO_MAX] dalle ultime [INFO_KEEP_S]. */
    private fun info(kept: List<News>, events: List<Event>, now: Long, l: Labels): List<News> {
        val fresh = events.mapNotNull { e ->
            when (e.kind) {
                EventKind.LAUNCHED -> e.session?.let { News("info:${e.key}", 3, Kind.LAUNCHED, it, l.launched.format(SpeakableName.of(it)), at = e.ts) }
                EventKind.RECAP -> e.body.lineSequence().firstOrNull { it.isNotBlank() }?.let {
                    News("info:${e.key}", 3, Kind.RECAP, null, l.recap.format(bare(it.replace(" · ", ", "), l)), at = e.ts)
                }
                else -> null
            }
        }
        return (kept + fresh.filter { f -> kept.none { it.key == f.key } })
            .filter { it.at >= now - INFO_KEEP_S }.sortedBy { it.at }.takeLast(INFO_MAX)
    }

    private fun sessionNews(s: Session, p: Session?, first: Boolean, now: Long, l: Labels): News? {
        val key = "s:${s.name}"
        val q = s.question
        val o = s.outcome
        return when {
            q != null -> if (p?.question?.id != q.id) News(key, 1, Kind.QUESTION, s.name, questionText(s, l), q.id, now) else null
            s.state == SessionState.GONE ->
                if (p != null && p.state != SessionState.GONE && !low(s)) goneNews(s.name, now, l) else null
            o != null && (if (p == null) !first || blocking(s) else p.outcome?.at != o.at) -> {
                val level = if (blocking(s)) 1 else 2
                if (level == 2 && low(s)) null else News(key, level, Kind.OUTCOME, s.name, outcomeText(s, l), at = now)
            }
            else -> null
        }
    }

    private fun goneNews(name: String, now: Long, l: Labels) =
        News("s:$name", 2, Kind.GONE, name, l.gone.format(SpeakableName.of(name)), at = now)

    /** «nome chiede: testo. Uno: opzione. Due: opzione.», accorciata a fine frase oltre [QUESTION_MAX]; mai «…». */
    private fun questionText(s: Session, l: Labels): String {
        val q = s.question ?: return ""
        val options = q.options.map { l.option.format(number(it.n, l).replaceFirstChar { c -> c.titlecase() }, bare(QuestionRules.optionText(it.label), l)) }
        val full = (listOf(l.question.format(SpeakableName.of(s.name), line(q.text, l))) + options).joinToString(" ")
        if (full.length <= QUESTION_MAX) return full
        val head = full.substring(0, QUESTION_MAX)
        val end = FINE_FRASE.findAll(head).lastOrNull()?.range?.last
        val start = if (end != null) head.substring(0, end + 1) else head.substringBeforeLast(' ').trimEnd(',', ';', ':', ' ') + "."
        return "$start ${l.more}"
    }

    /** «nome ha finito: esito breve.» più i Prossimi, prima quelli con «!». */
    private fun outcomeText(s: Session, l: Labels): String {
        val head = l.outcome.format(SpeakableName.of(s.name), bare(s.outcome?.short.orEmpty(), l))
        val steps = s.nextSteps.orEmpty().sortedBy { !it.blocking }
        if (steps.isEmpty()) return head
        return "$head " + l.next.format(steps.mapIndexed { i, st -> l.step.format(number(i + 1, l), bare(st.text, l)) }.joinToString("; "))
    }

    /** La richiesta di ok: titolo, cosa e dove. Il contratto 1.37 non dice quale sessione la chiede. */
    private fun approvalText(a: Approval, l: Labels): String {
        val where = a.where?.let { bare(it, l) }?.takeIf { it.isNotEmpty() }?.let { l.where.format(it) }
        val parts = listOfNotNull(a.title, a.what).map { bare(it, l) } + listOfNotNull(where)
        return l.approval.format(parts.filter { it.isNotEmpty() }.joinToString(", "))
    }

    /** La finestra più piena delle due, con l'ora in cui si azzera. */
    private fun quotaText(account: String, q: QuotaAccount, l: Labels, zone: ZoneId): String {
        val h5 = q.h5 ?: -1
        val w7 = q.w7 ?: -1
        val (pct, reset) = if (h5 >= w7) h5 to q.resetH5 else w7 to q.resetW7
        return reset?.let { l.quota.format(account, pct, HHMM.format(Instant.ofEpochSecond(it).atZone(zone))) }
            ?: l.quotaNoReset.format(account, pct)
    }

    private fun hot(q: QuotaAccount) = maxOf(q.h5 ?: 0, q.w7 ?: 0) >= QUOTA_HOT
    private fun low(s: Session) = s.lowPriority == LOW_ACTIVE
    private fun blocking(s: Session) = s.outcome != null && s.nextSteps.orEmpty().any { it.blocking }
    private fun number(n: Int, l: Labels) = l.numbers.getOrNull(n - 1) ?: n.toString()

    /** Il testo pulito per la voce, su una riga. */
    private fun line(t: String, l: Labels) = SpeechText.clean(t, l.code).replace('\n', ' ')

    /** Come `line`, senza il punto finale: il pezzo va dentro una frase. */
    private fun bare(t: String, l: Labels) = line(t, l).trimEnd('.', ',', ';', ':', ' ')
}
