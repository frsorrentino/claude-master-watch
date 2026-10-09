package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.AgendaPage
import it.pixelbox.cmwatch.contract.AgendaRow

/**
 * Le righe dell'agenda nel Recap (mockup approvato l'08/10, contratto 1.46): le aperte per chi deve muoversi — tu, Claude
 * (con «Fallo», che va alla master), altri — e in fondo tutte le altre (fatto, sospeso, chiuso o uno stato che non si
 * conosce, mostrato com'è). Il relay le manda nell'ordine del file: dentro ogni gruppo resta quell'ordine.
 */
object RecapAgenda {
    enum class Mark { PERSONAL, AGENCY, DESK, NONE }

    data class Model(val you: List<AgendaRow>, val claude: List<AgendaRow>, val other: List<AgendaRow>, val rest: List<AgendaRow>) {
        val isEmpty get() = you.isEmpty() && claude.isEmpty() && other.isEmpty() && rest.isEmpty()
    }

    /** Gli ambiti dei filtri, nell'ordine della pagina. */
    val SCOPES = listOf("agenzia", "personale", "postazione")

    private fun norm(s: String) = s.trim().lowercase()

    /** Il segno a sinistra: tondo personale, quadrato agenzia, rombo postazione. */
    fun mark(scope: String): Mark = when (norm(scope)) {
        "personale" -> Mark.PERSONAL
        "agenzia" -> Mark.AGENCY
        "postazione" -> Mark.DESK
        else -> Mark.NONE
    }

    /**
     * `blocks` è testo libero: sei tu il valore `owner` dell'agenda (contratto 1.47), o con un relay 1.46 «franz» (e «owner»
     * delle fixture); «claude» è Claude, il resto è fermo su altro.
     */
    fun isYou(blocks: String, owner: String? = null) = owner?.let { norm(blocks) == norm(it) } ?: (norm(blocks) in setOf("franz", "owner"))
    fun isClaude(blocks: String) = norm(blocks) == "claude"
    /** Aperta: `aperto`, o `sospeso` con `until` arrivato (contratto 1.47: la scheda rimandata torna quel giorno). */
    fun isOpen(row: AgendaRow, today: java.time.LocalDate? = null): Boolean = when (norm(row.state)) {
        "aperto" -> true
        "sospeso" -> today != null && row.until?.let { runCatching { java.time.LocalDate.parse(it.trim()) }.getOrNull() }?.let { !it.isAfter(today) } == true
        else -> false
    }

    /**
     * `scope` null = tutti gli ambiti. La riga d'intestazione del TSV (stato, ambito, …), se arriva, non è una riga; le
     * `scartato` restano nello storico del file ma qui non si vedono.
     */
    fun of(page: AgendaPage?, scope: String? = null, today: java.time.LocalDate? = null): Model {
        val rows = page?.rows.orEmpty()
            .filter { r -> r.title.isNotBlank() && norm(r.state) != "stato" && norm(r.state) != "scartato" }
            .filter { r -> scope == null || norm(r.scope) == norm(scope) }
        val open = rows.filter { isOpen(it, today) }
        val owner = page?.owner
        return Model(
            you = open.filter { isYou(it.blocks, owner) }, claude = open.filter { isClaude(it.blocks) },
            other = open.filter { !isYou(it.blocks, owner) && !isClaude(it.blocks) }, rest = rows.filterNot { isOpen(it, today) },
        )
    }

    /** Le voci del menu «Azioni» di una scheda, nell'ordine del menu (piano approvato il 09/10). */
    enum class Item { DEEPEN, DO, TALK, OPEN_REF, DONE, POSTPONE, PASS_CLAUDE, PASS_ME, REMOVE }

    /**
     * Le voci di una scheda: leggere, farla fare, parlarne e aprire il rimando sempre (fare solo se aperta); le scritture
     * nell'agenda solo con l'op del relay (`canWrite`), e su una scheda non aperta solo «Rimuovi».
     */
    fun menu(row: AgendaRow, canWrite: Boolean, today: java.time.LocalDate? = null): List<Item> {
        val open = isOpen(row, today) || norm(row.state) == "sospeso"
        return buildList {
            add(Item.DEEPEN)
            if (open) add(Item.DO)
            add(Item.TALK)
            if (row.ref.isNotBlank()) add(Item.OPEN_REF)
            // Senza key (relay 1.46) non si scrive: il relay non saprebbe quale riga.
            val write = canWrite && row.key.isNotBlank()
            if (write && open) {
                add(Item.DONE); add(Item.POSTPONE)
                add(if (isClaude(row.blocks)) Item.PASS_ME else Item.PASS_CLAUDE)
            }
            if (write) add(Item.REMOVE)
        }
    }

    private fun withRef(row: AgendaRow, base: String) = row.ref.trim().takeIf { it.isNotEmpty() }?.let { "$base ($it)" } ?: base

    /** Quello che va alla master quando la scheda non ha ancora il dettaglio: lo scrive lei, e la prossima volta c'è. */
    fun deepenText(row: AgendaRow, template: String = "Approfondisci: %1\$s") = withRef(row, template.format(row.title))

    /** L'inizio del messaggio alla master per «Parlane con la master»: la scheda citata, poi scrivi tu. */
    fun talkText(row: AgendaRow, template: String = "Sulla scheda «%1\$s»") = withRef(row, template.format(row.title)) + ": "

    private val TLD = setOf("com", "it", "net", "org", "io", "dev", "app", "eu", "ai", "co")

    /**
     * Il rimando come indirizzo da aprire, o null se è un file o un nome (allora lo manda la master). Vale il primo pezzo:
     * «console.anthropic.com -> API Keys» apre console.anthropic.com.
     */
    fun url(ref: String): String? {
        val first = ref.trim().substringBefore(' ')
        if (first.startsWith("https://") || first.startsWith("http://")) return first
        val host = first.substringBefore('/')
        val ok = Regex("^[a-z0-9-]+(\\.[a-z0-9-]+)+$", RegexOption.IGNORE_CASE).matches(host) && host.substringAfterLast('.').lowercase() in TLD
        return if (ok) "https://$first" else null
    }

    /** Una scrittura nell'agenda chiesta dal menu (Fatto, Rimanda, Rimuovi, Passa): la fa il relay con l'op del contratto 1.47. */
    data class Edit(val item: Item, val row: AgendaRow, val until: java.time.LocalDate? = null)

    /** Il comando `agenda_set` di una scrittura (contratto 1.47): azione, giorno e a chi passa. `owner` = tu nell'agenda. */
    data class SetCmd(val key: String, val action: String, val until: String? = null, val blocks: String? = null)
    fun setCmd(e: Edit, owner: String?): SetCmd? = when (e.item) {
        Item.DONE -> SetCmd(e.row.key, "done")
        Item.POSTPONE -> e.until?.let { SetCmd(e.row.key, "snooze", until = it.toString()) }
        Item.REMOVE -> SetCmd(e.row.key, "remove")
        Item.PASS_CLAUDE -> SetCmd(e.row.key, "pass", blocks = "claude")
        Item.PASS_ME -> SetCmd(e.row.key, "pass", blocks = owner ?: "franz")
        else -> null
    }?.takeIf { it.key.isNotBlank() }

    /** Rimanda: domani, o il lunedì della settimana dopo. */
    fun tomorrow(today: java.time.LocalDate): java.time.LocalDate = today.plusDays(1)
    fun nextWeek(today: java.time.LocalDate): java.time.LocalDate = today.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))

    /** «Fallo» su un lavoro che può fare Claude: alla master, dal foglio di conferma. `template` = «… %1$s …» col titolo e il rimando. */
    fun doIt(row: AgendaRow, template: String = "Fai questo lavoro dell'agenda: %1\$s%2\$s", refTemplate: String = " (%1\$s)"): RecapActions.Action {
        val ref = row.ref.trim().takeIf { it.isNotEmpty() }?.let { refTemplate.format(it) }.orEmpty()
        return RecapActions.Action(row.title, template.format(row.title, ref), ContextActions.MASTER, "agenda", viaMaster = true, agenda = true)
    }
}
