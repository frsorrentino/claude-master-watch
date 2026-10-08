package it.pixelbox.cmwatch.rules

/**
 * I consigli che la sessione scrive in fondo alla risposta quando si ferma (design 01/10, variante C): l'ultima riga
 * `Prossimi: a · b · c`. Si tolgono dal testo e diventano al massimo [MAX] righe sotto la risposta; un consiglio più lungo
 * di [MAX_CHARS] caratteri si scarta (sotto la risposta va su una riga). Conta solo l'ultima riga non vuota, saltando la
 * riga per l'orologio («Watch:»), che la master chiede dopo tutto il resto.
 */
object NextSteps {
    /** Dopo quante risposte di fila con la stessa azione questa diventa vecchia. */
    const val STALE_AFTER = 3

    const val MAX = 3
    const val MAX_CHARS = 40
    private const val PREFIX = "Prossimi:"
    private const val WATCH = "Watch:"

    /** Contratto 1.38: `blocking` = le voci scritte con «!» davanti, che sbloccano un lavoro fermo; il «!» si toglie. */
    data class Parsed(val text: String, val steps: List<String>, val blocking: Set<String> = emptySet())

    fun parse(text: String): Parsed {
        val lines = text.split('\n')
        val idx = lines.indices.reversed().firstOrNull { i -> lines[i].isNotBlank() && !lines[i].trimStart().startsWith(WATCH) }
            ?: return Parsed(text, emptyList())
        val line = lines[idx].trim()
        if (!line.startsWith(PREFIX)) return Parsed(text, emptyList())
        // Il «!» non conta nei 40 caratteri (regola 10 del kernel).
        val raw = line.removePrefix(PREFIX).split('·').map { it.trim() }
            .map { (it.startsWith("!")) to it.removePrefix("!").trim() }
            .filter { (_, s) -> s.isNotEmpty() && s.length <= MAX_CHARS }.take(MAX)
        val rest = (lines.take(idx) + lines.drop(idx + 1)).joinToString("\n").trimEnd()
        return Parsed(rest, raw.map { it.second }, raw.filter { it.first }.map { it.second }.toSet())
    }

    /** Il campo vuoto e il box «Prossimi» (Franz, 04/10 20:21): il suggerimento del campo e le righe che restano nel box. */
    data class Box(val field: String?, val rows: List<String>)

    /**
     * I consigli della sessione vengono prima del suggerito del terminale, che spesso ne copia uno: il primo fa da
     * suggerimento nel campo vuoto, gli altri stanno nel box senza doppioni, e il suggerito del terminale vi entra solo se
     * è diverso da tutti. Una riga già portata nel campo esce dal box e ci torna se il testo la perde; scrivendo altro,
     * anche quella del campo torna nel box. Senza consigli resta il suggerito del terminale nel campo, come prima.
     */
    fun box(steps: List<String>, terminal: String?, draft: String): Box {
        val t = terminal?.trim()?.takeIf { it.isNotEmpty() }
        if (steps.isEmpty()) return Box(t?.takeIf { draft.isBlank() }, emptyList())
        val all = (steps + listOfNotNull(t)).distinctBy(::norm)
        val field = all.first().takeIf { draft.isBlank() }
        return Box(field, all.filter { it != field && !inDraft(draft, it) }.take(MAX))
    }

    /**
     * Le azioni attuali e quelle vecchie (Franz, 08/10 20:50: «persistono anche se l'argomento è passato»). Quelle tolte a
     * mano (`dismissed`) non tornano; una che ricompare uguale nelle due risposte prima di questa, cioè in tre di fila, è
     * vecchia: sta in fondo, chiusa in una riga. `earlier` = le azioni delle risposte precedenti, dalla più recente.
     */
    data class Split(val fresh: List<String>, val old: List<String>)

    fun split(rows: List<String>, earlier: List<List<String>>, dismissed: Set<String>): Split {
        val gone = dismissed.map(::norm).toSet()
        val kept = rows.filter { norm(it) !in gone }
        val prev = earlier.take(STALE_AFTER - 1).map { l -> l.map(::norm).toSet() }
        val old = if (prev.size < STALE_AFTER - 1) emptyList() else kept.filter { r -> prev.all { norm(r) in it } }
        return Split(kept - old.toSet(), old)
    }

    /** Il testo con cui un'azione si ricorda come tolta: lo stesso confronto dei doppioni. */
    fun key(text: String): String = norm(text)

    /** Un testo già nel campo, a meno di maiuscole, spazi e punteggiatura. */
    fun inDraft(draft: String, text: String): Boolean = norm(text).let { it.isNotEmpty() && it in norm(draft) }

    /**
     * Una riga toccata col campo già scritto si accoda (Franz, 04/10 20:21): «fai X e poi prova dal vivo». La prima lettera
     * scende in minuscolo, tranne nelle sigle (CI, APK); la punteggiatura in fondo al testo di prima si toglie.
     */
    fun append(draft: String, step: String, then: String): String {
        val head = draft.trimEnd().trimEnd('.', ',', ';', ':').trimEnd()
        if (head.isEmpty()) return step
        val tail = if (step.length > 1 && step[0].isUpperCase() && step[1].isLowerCase()) step[0].lowercase() + step.substring(1) else step
        return "$head $then $tail"
    }

    private fun norm(s: String) = s.lowercase().replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
}
