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

    /** Riga da disegnare: `head` = apertura di blocco (colore e peso diversi); testo vuoto = stacco fra blocchi. */
    data class Row(val text: String, val head: Boolean = false)

    /** Chiamata a uno strumento come la stampa di Claude Code: `Bash(pytest -q)`, `Read(file.kt)`. */
    private val TOOL = Regex("^[A-Z][A-Za-z]{2,}\\(")

    /** Segni di apertura: prompt della shell, punti elenco di Claude Code, titoli markdown. */
    private val HEAD_CHARS = setOf('❯', '>', '$', '⏺', '✻', '•', '·', '#')

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
        for (riga in compatte.takeLast(MAX)) {
            val head = riga.isNotEmpty() && (riga[0] in HEAD_CHARS || TOOL.containsMatchIn(riga))
            // Stacco solo prima di un blocco nuovo e solo se sopra c'è del corpo: niente vuoti in cima né doppi.
            if (head && out.isNotEmpty() && out.last().text.isNotEmpty() && !out.last().head) out += Row("")
            out += Row(riga, head)
        }
        return out
    }
}
