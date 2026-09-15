package it.pixelbox.cmwatch.rules

import org.junit.Assert.*
import org.junit.Test

class TerminalTextTest {
    @Test fun indentazioneAzzerata() {
        val out = TerminalText.lines("def f():\n    return 1\n        # nota\n")
        // «# nota» apre un blocco, quindi ci va lo stacco davanti (13/09 17:46).
        assertEquals(listOf("def f():", "return 1", "", "# nota"), out)
    }

    @Test fun spaziInterniRidottiAUno() {
        assertEquals(listOf("nome stato durata"), TerminalText.lines("nome      stato     durata"))
    }

    @Test fun corniciDiTmuxVia() {
        val out = TerminalText.lines("│ pytest -q          │\n╰────╯")
        assertEquals(listOf("pytest -q"), out)
    }

    @Test fun codiciAnsiVia() {
        assertEquals(listOf("ok 12 passed"), TerminalText.lines("\u001B[32mok\u001B[0m 12 passed"))
    }

    @Test fun righeVuoteAlMassimoUnaDiFila() {
        assertEquals(listOf("a", "", "b"), TerminalText.lines("a\n\n\n\nb\n\n"))
    }

    @Test fun tieneSoloLeUltimeTrenta() {
        val out = TerminalText.lines((1..50).joinToString("\n") { "riga $it" })
        assertEquals(TerminalText.MAX, out.size)
        assertEquals("riga 50", out.last())
        assertEquals("riga 21", out.first())
    }

    @Test fun leTabulazioniDiventanoSpazi() {
        assertEquals(listOf("a b"), TerminalText.lines("\ta\tb"))
    }

    @Test fun tieneLeRigheIntereDelComando() {
        assertEquals(
            listOf("$ pytest -q tests", "42 passed in 3.1s"),
            TerminalText.lines("$ pytest -q tests" + "\n" + "42 passed in 3.1s" + "\n"),
        )
    }
}

class TerminalRowsTest {
    @Test fun prompt_strumento_elenco_e_titolo_sono_teste() {
        val out = TerminalText.rows("testo normale\n❯ pytest -q\nBash(pytest -q)\n• punto\n# titolo")
        val teste = out.filter { it.head }.map { it.text }
        assertEquals(listOf("❯ pytest -q", "Bash(pytest -q)", "• punto", "# titolo"), teste)
    }

    @Test fun staccoSoloPrimaDiUnBloccoNuovo() {
        val out = TerminalText.rows("riga di corpo\n❯ pytest -q\nok")
        assertEquals(listOf("riga di corpo", "", "❯ pytest -q", "ok"), out.map { it.text })
    }

    @Test fun nienteStaccoInCimaNeFraDueTeste() {
        val out = TerminalText.rows("❯ uno\n❯ due")
        assertEquals(listOf("❯ uno", "❯ due"), out.map { it.text })
    }

    @Test fun leRigheRestanoQuelleDelTestoCompattato() {
        assertEquals(TerminalText.rows("a\nb").map { it.text }, TerminalText.lines("a\nb"))
    }
}

class TerminalLineNumbersTest {
    @Test fun numeriDiRigaInSequenzaVia() {
        val out = TerminalText.lines("122 relay push\n123 relay pair\n124 relay serve")
        assertEquals(listOf("relay push", "relay pair", "relay serve"), out)
    }

    @Test fun numeroSingoloResta() {
        assertEquals(listOf("42 passed in 3.1s"), TerminalText.lines("42 passed in 3.1s"))
    }

    @Test fun formaConDuePuntiVia() {
        // Ne servono almeno tre: due numeri non bastano a distinguerli da un dato che inizia per cifra.
        val out = TerminalText.lines("10: prima\n11: seconda\n12: terza")
        assertEquals(listOf("prima", "seconda", "terza"), out)
    }

    @Test fun lOraNonEUnNumeroDiRiga() {
        val out = TerminalText.lines("12:34 avviato\n12:35 finito")
        assertEquals(listOf("12:34 avviato", "12:35 finito"), out)
    }
}

class TerminalNoiseTest {
    @Test fun corniceDiClaudeCodeVia() {
        val grezzo = listOf(
            "❯ pytest -q",
            "42 passed",
            "? for shortcuts",
            "esc to interrupt",
            "Context left until auto-compact: 23%",
            "⏵⏵ bypass permissions on",
            "cwd: /home/demo/progetto",
        ).joinToString("\n")
        assertEquals(listOf("❯ pytest -q", "42 passed"), TerminalText.lines(grezzo))
    }

    @Test fun prefissiAdAlberoVia() {
        val out = TerminalText.lines("⎿ Updated SKILL.md (+2 -1)")
        assertEquals(listOf("Updated SKILL.md (+2 -1)"), out)
    }

    @Test fun rigaRipetutaDelloSpinnerUnaSola() {
        assertEquals(listOf("Thinking"), TerminalText.lines("Thinking\nThinking\nThinking"))
    }
}

class TerminalLineNumbersHardTest {
    @Test fun numeriNonAdiacentiViaComunque() {
        val grezzo = listOf(
            "122 relay push|pair|serve",
            "continuazione della riga",
            "123 altra riga di file",
            "ancora continuazione",
            "124 terza riga",
        ).joinToString("\n")
        val out = TerminalText.lines(grezzo)
        assertEquals(
            listOf("relay push|pair|serve", "continuazione della riga", "altra riga di file", "ancora continuazione", "terza riga"),
            out,
        )
    }

    @Test fun statisticheNonSonoNumeriDiRiga() {
        val out = TerminalText.lines("42 passed\n13 failed\n5 skipped")
        assertEquals(listOf("42 passed", "13 failed", "5 skipped"), out)
    }
}

class TerminalNoiseVistoDalVivoTest {
    @Test fun scorciatoieSuggerimentiESpinnerVia() {
        val grezzo = listOf(
            "❯ claude-master relay push",
            "(ctrl+b ctrl+b (twice) to run in background)",
            "● Metamorphosing… (7m 46s · ↓ 7.8k tokens)",
            "Tip: Use /memory to view and manage Claude memory",
            "push fatto",
        ).joinToString("\n")
        assertEquals(listOf("❯ claude-master relay push", "push fatto"), TerminalText.lines(grezzo))
    }
}

/** Il terminale come un copione (design 15/09): chi parla si vede dalla voce della riga, non da una bolla. */
class TerminalVociTest {
    private val USER = TerminalText.Kind.USER
    private val CLAUDE = TerminalText.Kind.CLAUDE
    private val TOOL = TerminalText.Kind.TOOL
    private val OUTPUT = TerminalText.Kind.OUTPUT

    @Test fun leVociDelTerminale() {
        val out = TerminalText.rows(listOf(
            "❯ Lancia i test e poi", "aggiorna il changelog", "⏺ Lancio la suite.",
            "⏺ Bash(pytest -q)", "⎿ 42 passed in 3.1s", "⏺ Tutto verde, passo al", "changelog.",
        ).joinToString("\n")).filter { it.text.isNotEmpty() }
        assertEquals(listOf(USER, USER, CLAUDE, TOOL, OUTPUT, CLAUDE, CLAUDE), out.map { it.kind })
        assertEquals("Lancia i test e poi", out[0].shown)
        assertEquals("Bash(pytest -q)", out[3].shown)
    }

    @Test fun laRispostaAUnaDomandaEDellUtente() {
        val out = TerminalText.rows("● User answered Claude's questions:\n· Prova dal polso? → Continua\n⏺ Continuo.")
            .filter { it.text.isNotEmpty() }
        assertEquals(listOf(USER, USER, CLAUDE), out.map { it.kind })
    }

    @Test fun ilPromptVuotoInFondoSparisce() {
        assertEquals(listOf("⏺ Fatto."), TerminalText.lines("⏺ Fatto.\n❯ "))
    }

    @Test fun primaDellaPrimaTestaEOutput() {
        assertEquals(OUTPUT, TerminalText.rows("42 passed\n❯ ok").first().kind)
    }

    @Test fun blocchiPerVoce() {
        val b = TerminalText.blocks("❯ Lancia i test e poi\naggiorna il changelog\n⏺ Lancio la suite.\n⏺ Bash(pytest -q)\n⎿ 42 passed in 3.1s\n⏺ Tutto verde.")
        // La prosa (tua e di Claude) va a capo dove va a capo il polso, non dove andava il PC (una riga logica, una fisica).
        assertEquals(listOf(
            USER to "Lancia i test e poi aggiorna il changelog", CLAUDE to "Lancio la suite.",
            TOOL to "Bash(pytest -q)", OUTPUT to "42 passed in 3.1s", CLAUDE to "Tutto verde.",
        ), b.map { it.kind to it.text })
    }

    @Test fun lElencoRestaARighe() {
        val b = TerminalText.blocks("⏺ Due cose:\n• prima\n• seconda")
        assertEquals(listOf(CLAUDE to "Due cose:\n• prima\n• seconda"), b.map { it.kind to it.text })
    }

    @Test fun loutputRestaARighe() {
        val b = TerminalText.blocks("⏺ Bash(ls)\n⎿ a.kt\nb.kt")
        assertEquals(OUTPUT to "a.kt\nb.kt", b.last().kind to b.last().text)
    }

    @Test fun ilTitoloEUnBloccoASe() {
        val b = TerminalText.blocks("⏺ Fatto:\n# Esito\nTutto verde")
        assertEquals(listOf(false, true, false), b.map { it.heading })
    }
}
