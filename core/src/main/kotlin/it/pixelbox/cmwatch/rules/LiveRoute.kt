package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import java.text.Normalizer

/**
 * Lo smistamento del tasto «Parla» (specifica live, §6): il testo dettato diventa un'istruzione per una sessione, una
 * domanda sullo stato, l'apertura di una doppia conferma o una domanda per la master. Regole fisse, senza modello: vale
 * la prima che si applica, e il testo si confronta senza maiuscole né accenti. Qui niente invio: la rotta dice cosa
 * fare, il servizio lo fa (la voce, Annulla per 5 secondi, il `prompt`).
 */
object LiveRoute {
    /** Cosa si stava chiedendo quando un nome ha più candidati. */
    enum class Intent { TELL, STATUS, APPROVE, ACTION }

    sealed class Route {
        /** «Mando a nome: testo», Annulla per 5 secondi, poi `prompt` alla sessione. */
        data class Tell(val session: String, val text: String) : Route()

        /** Istruzione senza testo: «Cosa dico a nome?», e si aspetta un altro «Parla». */
        data class AskText(val session: String) : Route()

        /** Più candidati: la voce dice «uno: …, due: …» e il watch mostra i tasti; `text` = il resto dell'istruzione. */
        data class Pick(val intent: Intent, val options: List<String>, val text: String = "") : Route()

        /** «non trovo nome». */
        data class NotFound(val name: String) : Route()

        /** Lo stato di una sessione, con le frasi del notiziario. */
        data class Status(val session: String) : Route()

        /** Le sessioni con il livello 1 in sospeso. */
        data object Waiting : Route()
        data object Quota : Route()
        data object Round : Route()

        /** Apre soltanto la doppia conferma (`DoubleConfirm`): a voce un ok non si completa mai. */
        data class Approve(val task: String) : Route()

        /** Tutto il resto: `prompt` alla sessione «master», marcato a voce. */
        data class Master(val text: String) : Route()

        /** Trascrizione vuota: «non ho capito» e il tasto Riprova. */
        data object Empty : Route()
    }

    /** «di' a», «dì a», «di a», «scrivi a», «chiedi a», «manda a»; anche con «ad» davanti a vocale. Dopo `fold`. */
    private val TELL = listOf("di' a ", "di a ", "scrivi a ", "chiedi a ", "manda a ", "di' ad ", "di ad ", "scrivi ad ", "chiedi ad ", "manda ad ")
    private val STATUS = listOf("com'e messa ", "come e messa ", "come va ", "a che punto e ")
    private val WAITING = listOf("chi aspetta me", "chi mi aspetta")
    private val QUOTA = listOf("quanta quota")
    private val ROUND = listOf("giro completo", "fammi il giro")
    private const val APPROVE = "approva"

    /** Una parola più corta non si cerca per prefisso né dentro i nomi: «la» troverebbe «atlas shop». */
    private const val MIN_PART = 3
    private val WORD = Regex("^[\\p{L}\\p{N}]+")
    private val ARTICLE = Regex("^(?:(?:il|lo|la|i|gli|le|un|una|uno)\\s+|l')")

    fun parse(text: String, state: State): Route {
        val raw = text.trim()
        if (raw.isEmpty()) return Route.Empty
        val t = fold(raw)
        TELL.firstOrNull { t.startsWith(it) }?.let { p ->
            val open = state.sessions.filter { it.state != SessionState.GONE }.map { it.name }
            return when (val m = resolve(raw.substring(p.length), t.substring(p.length), open)) {
                is Match.Found -> if (m.rest.isEmpty()) Route.AskText(m.name) else Route.Tell(m.name, m.rest)
                is Match.Many -> Route.Pick(Intent.TELL, m.names, m.rest)
                is Match.None -> Route.NotFound(m.word)
            }
        }
        // Senza una sessione riconosciuta la regola dello stato non si applica, e si passa alle successive.
        STATUS.firstOrNull { t.startsWith(it) }?.let { p ->
            when (val m = resolve(raw.substring(p.length), t.substring(p.length), state.sessions.map { it.name })) {
                is Match.Found -> return Route.Status(m.name)
                is Match.Many -> return Route.Pick(Intent.STATUS, m.names)
                is Match.None -> Unit
            }
        }
        if (WAITING.any { t.startsWith(it) }) return Route.Waiting
        if (QUOTA.any { t.startsWith(it) }) return Route.Quota
        if (ROUND.any { t.startsWith(it) }) return Route.Round
        if (t == APPROVE || t.startsWith("$APPROVE ")) return approve(raw.substring(APPROVE.length), state)
        return Route.Master(raw)
    }

    /** Minuscole, senza accenti, apostrofo dritto. Carattere per carattere: le posizioni restano quelle del testo dettato. */
    fun fold(s: String): String = buildString(s.length) {
        for (c in s) append(
            when (c) {
                '’', '‘', '`', '´' -> '\''
                else -> Normalizer.normalize(c.lowercaseChar().toString(), Normalizer.Form.NFD).first()
            },
        )
    }

    private sealed class Match {
        data class Found(val name: String, val rest: String) : Match()
        data class Many(val names: List<String>, val rest: String) : Match()
        data class None(val word: String) : Match()
    }

    /**
     * Il nome in testa a `orig` (con `folded` la stessa stringa dopo `fold`) fra i nomi pronunciabili delle sessioni:
     * prima l'uguaglianza (il nome intero, a fine parola; vince il più lungo), poi il prefisso, poi il contenuto della
     * prima parola. `rest` = il testo dopo il nome, senza la punteggiatura davanti.
     */
    private fun resolve(orig: String, folded: String, names: List<String>): Match {
        val lead = folded.length - folded.trimStart().length
        val o = orig.substring(lead)
        val f = folded.substring(lead)
        val spoken = names.map { it to fold(SpeakableName.of(it)) }.filter { it.second.isNotEmpty() }
        val exact = spoken.filter { (_, s) -> f.startsWith(s) && (f.length == s.length || !f[s.length].isLetterOrDigit()) }
        if (exact.isNotEmpty()) {
            val len = exact.maxOf { it.second.length }
            return pick(exact.filter { it.second.length == len }.map { it.first }, rest(o, len))
        }
        val word = WORD.find(f)?.value ?: return Match.None("")
        if (word.length >= MIN_PART) {
            val prefix = spoken.filter { it.second.startsWith(word) }.map { it.first }
            if (prefix.isNotEmpty()) return pick(prefix, rest(o, word.length))
            val inside = spoken.filter { word in it.second }.map { it.first }
            if (inside.isNotEmpty()) return pick(inside, rest(o, word.length))
        }
        return Match.None(o.substring(0, word.length))
    }

    private fun pick(names: List<String>, rest: String): Match =
        if (names.size == 1) Match.Found(names.single(), rest) else Match.Many(names, rest)

    private fun rest(o: String, from: Int) = o.substring(from).trimStart(':', ',', ';', '.', '-', ' ').trim()

    /** «approva X»: la richiesta in `approvals` per titolo o compito; senza X, l'unica che c'è. */
    private fun approve(orig: String, state: State): Route {
        val said = orig.trim().trimEnd('.', '?', '!', ' ')
        val f = fold(said).replace(ARTICLE, "")
        val found = state.approvals.filter { a -> f.isEmpty() || f in fold(a.title) || f in fold(a.task).replace('-', ' ') || f in fold(a.task) }
        return when (found.size) {
            0 -> Route.NotFound(said)
            1 -> Route.Approve(found.single().task)
            else -> Route.Pick(Intent.APPROVE, found.map { it.task })
        }
    }
}
