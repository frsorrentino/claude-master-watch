package it.pixelbox.cmwatch.rules

/**
 * Terminale al polso: le ultime righe della cattura del PC, riformattate per uno schermo da ventotto caratteri
 * (Franz, 13/09 17:19). Via i codici ANSI e le cornici di tmux, indentazione azzerata, spazi interni ridotti a uno,
 * righe vuote non più di una di fila. L'indentazione su questa larghezza è spazio buttato: la struttura resta nei
 * segni propri della riga, trattini, numeri e due punti.
 *
 * Per la leggibilità (Franz, 13/09 17:46) le righe che aprono un blocco — prompt, chiamata a uno strumento, punto
 * elenco, titolo — si riconoscono e si marcano come «testa»: la schermata le colora e le ingrossa, e lo stacco va
 * solo PRIMA di un nuovo blocco. Una riga vuota dopo ogni periodo costerebbe una riga su trenta per ogni frase:
 * qui lo stacco lo fa la gerarchia, non il vuoto.
 */
object TerminalText {
    const val MAX = 30

    /** Chi parla nella riga (design 15/09, «copione»): tu, Claude che scrive, una chiamata a strumento, il suo output. */
    enum class Kind { USER, CLAUDE, TOOL, OUTPUT }

    /** Riga da disegnare: `head` = apertura di blocco; testo vuoto = stacco fra blocchi; `kind` = chi parla. */
    data class Row(val text: String, val head: Boolean = false, val kind: Kind = Kind.OUTPUT) {
        /** Il testo senza il segno di chi parla: sul polso lo dicono carattere e colore. `text` resta intero per la voce. */
        val shown: String get() = if (text.isNotEmpty() && text[0] in MARKS) text.drop(1).trimStart() else text
    }

    /** Righe consecutive della stessa voce in un solo item: così il filo accanto alle righe dell'utente resta continuo. */
    data class Block(val kind: Kind, val text: String, val heading: Boolean = false)

    /** Chiamata a uno strumento come la stampa di Claude Code: `Bash(pytest -q)`, `Read(file.kt)`. */
    private val TOOL = Regex("^[A-Z][A-Za-z]{2,}\\(")

    /** Segni di apertura: prompt della shell, punti elenco di Claude Code, titoli markdown. */
    private val HEAD_CHARS = setOf('❯', '›', '>', '$', '⏺', '●', '✻', '•', '·', '#')

    /** Segni di chi parla, tolti dal testo disegnato: prompt dell'utente e punto di Claude Code. */
    private val MARKS = setOf('❯', '›', '>', '⏺', '●')
    private val USER_MARKS = setOf('❯', '›', '>')
    private val SPEAKS = setOf('⏺', '●')

    /** Il prompt vuoto in fondo alla cattura: sul polso sarebbe un filo azzurro con niente accanto. */
    private val BARE_PROMPTS = setOf("❯", "›", ">")

    /** Le risposte alle domande: le stampa Claude Code, ma le ha date l'utente. */
    private val ANSWERED = Regex("^[⏺●]?\\s*User answered")

    /** Codici ANSI: la cattura arriva grezza dal PC e i colori qui sono solo rumore. */
    private val ANSI = Regex("\u001B\\[[0-9;?]*[A-Za-z]")

    /** Cornici di tmux e di Claude Code a inizio e fine riga: rubano larghezza e non dicono nulla. */
    private val BORDER = Regex("^[\\s\u2500-\u259F|]+|[\\s\u2500-\u259F|]+$")

    private val SPACES = Regex(" {2,}")

    /**
     * Numeri di riga che entrano nella cattura (`cat -n`, `grep -n`, la lettura dei file di Claude Code). Al polso
     * fanno solo confusione (Franz, 13/09 17:50), ma si toglie solo quando sono una sequenza: un «42 passed» resta.
     */
    private val LINENO = Regex("^(\\d{1,6})(\\t| {1,3}|: )")

    /**
     * Cornice del terminale di Claude Code: suggerimenti dei tasti, banner dei permessi, riga del contesto, conti di
     * spesa. Sul PC servono, al polso sono rumore e mangiano righe (Franz, 13/09 17:53).
     */
    private val NOISE = listOf(
        Regex("for shortcuts"), Regex("to interrupt"), Regex("to exit"), Regex("to edit"),
        Regex("^cwd:", RegexOption.IGNORE_CASE), Regex("^/help"), Regex("auto-compact"),
        Regex("bypass permissions"), Regex("accept edits on"), Regex("plan mode on"),
        Regex("^Total (cost|duration)", RegexOption.IGNORE_CASE), Regex("tokens? (used|left|remaining)"),
        Regex("^Press "), Regex("^Try \""), Regex("^Welcome to Claude Code"),
        // Visto nella cattura vera (13/09 18:35): scorciatoie, suggerimenti e la riga dello spinner con i token.
        Regex("ctrl\\+[a-z]"), Regex("^Tip: "), Regex("to run in background"),
        Regex("\\d+(\\.\\d+)?k tokens"), Regex("^[●•✻✳*] \\p{L}+…"),
    )

    /** Prefissi ad albero della stampa di Claude Code: il testo resta, la grafica va. */
    private val TREE = Regex("^[⎿└├┤┬┴┼╰╭│┃|]+\\s*")

    private fun senzaRumore(righe: List<String>): List<String> {
        val out = mutableListOf<String>()
        for (riga in righe) {
            val r = TREE.replace(riga, "").trim()
            if (NOISE.any { it.containsMatchIn(r) }) continue
            // Lo spinner ripete la stessa riga: una basta.
            if (out.isNotEmpty() && out.last() == r) continue
            out += r
        }
        return out
    }

    /**
     * I numeri di riga si riconoscono sul testo intero, non riga per riga: fra due righe numerate ce ne stanno altre
     * andate a capo, quindi i numeri non sono adiacenti (Franz, 13/09 18:11: «mostra ancora il numero di righe»).
     * Si toglie solo se sono almeno tre e in gran parte crescenti: così «42 passed / 13 failed / 5 skipped» resta.
     */
    private fun senzaNumeriDiRiga(righe: List<String>): List<String> {
        val numeri = righe.mapNotNull { LINENO.find(it)?.groupValues?.get(1)?.toIntOrNull() }
        if (numeri.size < 3) return righe
        val crescenti = numeri.zipWithNext().count { (a, b) -> b > a }
        if (crescenti * 10 < (numeri.size - 1) * 7) return righe
        return righe.map { LINENO.replace(it, "").trim() }
    }

    fun lines(text: String): List<String> = rows(text).map { it.text }

    fun rows(text: String): List<Row> {
        val pulite = text.replace("\t", " ").lines().map { riga ->
            SPACES.replace(BORDER.replace(ANSI.replace(riga, ""), ""), " ").trim()
        }
        val compatte = mutableListOf<String>()
        for (riga in senzaNumeriDiRiga(senzaRumore(pulite))) {
            if (riga.isEmpty() && (compatte.isEmpty() || compatte.last().isEmpty())) continue
            compatte += riga
        }
        while (compatte.isNotEmpty() && compatte.last().isEmpty()) compatte.removeAt(compatte.size - 1)

        val out = mutableListOf<Row>()
        var voce = Kind.OUTPUT
        for (riga in compatte.takeLast(MAX)) {
            if (riga in BARE_PROMPTS) continue
            val head = riga.isNotEmpty() && (riga[0] in HEAD_CHARS || TOOL.containsMatchIn(riga))
            voce = voceDi(riga, voce)
            // Stacco solo prima di un blocco nuovo e solo se sopra c'è del corpo: niente vuoti in cima né doppi.
            if (head && out.isNotEmpty() && out.last().text.isNotEmpty() && !out.last().head) out += Row("", kind = voce)
            out += Row(riga, head, voce)
        }
        // Tolto il prompt vuoto in fondo, non deve restare lo stacco che lo precedeva.
        while (out.isNotEmpty() && out.last().text.isEmpty()) out.removeAt(out.size - 1)
        return out
    }

    /**
     * Chi parla: i segni propri della stampa di Claude Code decidono la testa del blocco, il corpo eredita la voce della
     * sua testa. Sotto una chiamata a strumento il corpo è il suo output; prima della prima testa, output anche lui.
     */
    private fun voceDi(riga: String, corrente: Kind): Kind = when {
        riga.isEmpty() -> corrente
        ANSWERED.containsMatchIn(riga) -> Kind.USER
        riga[0] in USER_MARKS -> Kind.USER
        riga[0] in SPEAKS -> if (TOOL.containsMatchIn(riga.drop(1).trimStart())) Kind.TOOL else Kind.CLAUDE
        riga[0] == '$' || TOOL.containsMatchIn(riga) -> Kind.TOOL
        riga[0] == '#' -> Kind.CLAUDE
        // Un elenco resta di chi lo sta scrivendo: le risposte «· … → …» sono dell'utente, i punti sotto la prosa di Claude.
        riga[0] == '•' || riga[0] == '·' -> if (corrente == Kind.TOOL || corrente == Kind.OUTPUT) Kind.CLAUDE else corrente
        riga[0] == '✻' -> Kind.OUTPUT
        corrente == Kind.TOOL -> Kind.OUTPUT
        else -> corrente
    }

    /** Le righe della prosa unite da uno spazio: gli a capo li decide il polso, non la larghezza del PC. Un elenco resta a righe. */
    private fun prosa(righe: List<String>): String = buildString {
        righe.forEachIndexed { i, r ->
            if (i > 0) append(if (r.isNotEmpty() && (r[0] in LIST_MARKS || NUMBERED.containsMatchIn(r))) "\n" else " ")
            append(r)
        }
    }

    private val LIST_MARKS = setOf('•', '·', '-', '*')
    private val NUMBERED = Regex("^\\d+[.)] ")

    /**
     * I blocchi da disegnare: righe non vuote consecutive della stessa voce, spezzate dallo stacco, dal cambio di voce e
     * dai titoli `#` (un blocco a sé, in grassetto, senza i cancelletti).
     */
    fun blocks(text: String): List<Block> {
        val out = mutableListOf<Block>()
        var righe = mutableListOf<String>()
        var voce: Kind? = null
        var titolo = false
        fun chiudi() {
            val v = voce
            if (v != null && righe.isNotEmpty()) out += Block(v, if (v == Kind.USER || v == Kind.CLAUDE) prosa(righe) else righe.joinToString("\n"), titolo)
            righe = mutableListOf(); voce = null; titolo = false
        }
        for (r in rows(text)) {
            if (r.text.isEmpty()) { chiudi(); continue }
            val tit = r.text[0] == '#'
            if (voce != r.kind || tit || titolo) chiudi()
            voce = r.kind; titolo = tit
            righe += if (tit) r.text.trimStart('#').trim() else r.shown
        }
        chiudi()
        return out
    }
}
