package it.pixelbox.cmwatch.rules

/**
 * La risposta intera della sessione (contratto 1.4, `last`) divisa in blocchi da leggere e da ascoltare uno per volta
 * (Franz, 15/09 17:19: «non solo sentire tutto ma anche sentire per blocchi/paragrafi»). Paragrafi separati da una riga
 * vuota, ogni voce d'elenco un blocco, titoli markdown o righe tutte in grassetto come titoli, blocchi ``` come codice.
 * Il grassetto si toglie; i `backtick` restano, li disegna l'interfaccia in mono.
 */
object AnswerText {
    enum class Kind { PARA, BULLET, HEADING, CODE }

    data class Block(val kind: Kind, val text: String)

    private val TITOLO = Regex("^#{1,6}\\s+(.+)$")
    private val GRASSETTO = Regex("^\\*\\*(.+?)\\*\\*:?$")
    private val VOCE = Regex("^\\s*(?:[-*•]|\\d+[.)])\\s+(.+)$")
    private val ENFASI = Regex("\\*\\*|__")

    fun blocks(text: String): List<Block> {
        val out = mutableListOf<Block>()
        val para = StringBuilder()
        var code: StringBuilder? = null
        var afterBullet = false
        fun flush() {
            if (para.isNotBlank()) out += Block(Kind.PARA, pulito(para.toString()))
            para.clear()
        }
        for (raw in text.lines()) {
            val line = raw.trimEnd()
            val c = code
            if (c != null) {
                if (line.trimStart().startsWith("```")) { out += Block(Kind.CODE, c.toString().trimEnd('\n')); code = null }
                else c.append(line).append('\n')
                continue
            }
            val t = line.trim()
            val voce = VOCE.find(line)
            when {
                t.startsWith("```") -> { flush(); code = StringBuilder(); afterBullet = false }
                t.isEmpty() -> { flush(); afterBullet = false }
                TITOLO.matches(t) -> { flush(); out += Block(Kind.HEADING, pulito(TITOLO.find(t)!!.groupValues[1])); afterBullet = false }
                GRASSETTO.matches(t) -> { flush(); out += Block(Kind.HEADING, pulito(GRASSETTO.find(t)!!.groupValues[1])); afterBullet = false }
                voce != null -> { flush(); out += Block(Kind.BULLET, pulito(voce.groupValues[1])); afterBullet = true }
                // Una riga rientrata sotto una voce d'elenco la continua.
                afterBullet && raw.startsWith(" ") -> { val b = out.removeAt(out.lastIndex); out += b.copy(text = b.text + " " + pulito(t)) }
                else -> { if (para.isNotEmpty()) para.append(' '); para.append(t); afterBullet = false }
            }
        }
        code?.let { if (it.isNotBlank()) out += Block(Kind.CODE, it.toString().trimEnd('\n')) }
        flush()
        return out.filter { it.text.isNotBlank() }
    }

    private fun pulito(t: String) = t.replace(ENFASI, "").trim()
}
