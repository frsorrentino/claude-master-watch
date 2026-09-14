package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.CmdResult
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Outcome
import it.pixelbox.cmwatch.contract.Recap
import it.pixelbox.cmwatch.contract.RecapItem
import org.junit.Assert.*
import org.junit.Test

/** Testi da leggere a voce (Franz, 14/09 12:17, «applica tutte»): niente simboli letti, pezzi a fine frase. */
class SpeechTextTest {
    private val code = "segue un blocco di codice"

    @Test fun ilCodiceSiAnnunciaENonSiLegge() {
        val t = SpeechText.clean("Ecco la patch:\n```kotlin\nval x = 1\n```\nFatto.", code)
        assertFalse(t.contains("```")); assertFalse(t.contains("val x"))
        assertTrue(t.contains(code)); assertTrue(t.endsWith("Fatto."))
    }

    @Test fun ilMarkdownNonSiLeggeASimboli() {
        val t = SpeechText.clean("## Stato\n- **tile** rifatta\n- `quotaLine` nuova\nVedi [il piano](docs/plans/x.md).", code)
        assertEquals("Stato.\ntile rifatta.\nquotaLine nuova.\nVedi il piano.", t)
    }

    @Test fun leTabelleDiventanoElenchi() {
        val t = SpeechText.clean("| conto | 5 ore |\n|---|---|\n| personale | 9 % |", code)
        assertEquals("conto, 5 ore.\npersonale, 9 %.", t)
    }

    @Test fun unTestoCortoEUnPezzoSolo() = assertEquals(listOf("Tutto a posto."), SpeechText.chunks("Tutto a posto."))

    @Test fun iPezziSiTaglianoAFineFrase() =
        assertEquals(listOf("Uno due tre.", "Quattro cinque sei. Sette."), SpeechText.chunks("Uno due tre. Quattro cinque sei. Sette.", max = 30))

    @Test fun unTestoLungoNonPerdeNiente() {
        val testo = (1..400).joinToString(" ") { "Frase numero $it della risposta." }
        val pezzi = SpeechText.chunks(testo, max = 500)
        assertTrue(pezzi.size > 1); assertTrue(pezzi.all { it.length <= 500 })
        assertEquals(testo, pezzi.joinToString(" "))
    }

    @Test fun unaFraseSenzaPuntiSiTagliaAUnoSpazio() {
        val frase = (1..200).joinToString(" ") { "parola$it" }
        val pezzi = SpeechText.chunks(frase, max = 100)
        assertTrue(pezzi.size > 1); assertTrue(pezzi.all { it.length <= 100 })
        assertEquals(frase, pezzi.joinToString(" "))
    }

    @Test fun laDomandaSiLeggeConLeOpzioniNumerate() {
        val q = ContractJson.decodeState(Fixtures.stateQuestion).sessions.first { it.question != null }.question!!
        val attesa = q.text + " " + q.options.joinToString(" ") { "${it.n}, ${it.label}." }
        assertEquals(attesa, SpeechText.question(q, "%1\$d, %2\$s."))
    }

    @Test fun ilRecapDiceFattoEProssimoPerProgetto() {
        val r = Recap("14/09", listOf(RecapItem("atlas-shop", "deploy fatto", "test e2e"), RecapItem("orbit-docs", "indice rifatto.", null)))
        assertEquals("atlas-shop: deploy fatto. Prossimo: test e2e. orbit-docs: indice rifatto.", SpeechText.recap(r, "Prossimo: %1\$s."))
    }

    @Test fun lEsitoLeggeSoloLaFraseDEsito() {
        val o = Outcome("relay aggiornato", "Ho cambiato fit_state.\nEsito: relay aggiornato alla 1.6, short fino a 200.", 0)
        assertEquals("relay aggiornato alla 1.6, short fino a 200.", SpeechText.outcome(o))
    }

    @Test fun unEsitoSenzaRigaDEsitoLeggeIlTitolo() =
        assertEquals("Fatto.", SpeechText.outcome(Outcome("Fatto.", "Fatto.", 0)))

    private val cattura = listOf(
        "● Mi mancano ancora il meccanismo di caricamento config in cm-lib.sh e i",
        "  nomi delle chiavi: procedo.",
        "● Running 1 shell command…",
        "  ⎿  \$ cd /home/demo; sed -n",
        "     1,20p CHANGELOG.md",
        "✻ Waiting for API response · will retry in 2m 40s",
        "──────────────────────────── claude-master ─",
        "❯ ",
        "────────────────────────────────────────────",
        "  Opus 5 · quota 8%, resets 17:30 · context 8% │ [CAVEMAN]",
        "  └ weekly quota 60% used, resets 17 Sep",
    ).joinToString("\n")

    @Test fun ilTerminaleLeggeGliUltimiBlocchiSopraIlPromptSenzaComandi() = assertEquals(
        "Mi mancano ancora il meccanismo di caricamento config in cm-lib.sh e i nomi delle chiavi: procedo.\n" +
            "Running 1 shell command…\nWaiting for API response · will retry in 2m 40s",
        SpeechText.terminal(cattura),
    )

    @Test fun delTerminaleSiLeggonoAlPiuTreBlocchi() {
        val t = (1..6).joinToString("\n") { "● blocco $it" } + "\n❯ "
        assertEquals("blocco 4\nblocco 5\nblocco 6", SpeechText.terminal(t))
    }

    @Test fun siLeggeLaRispostaDelPcQuandoCe() =
        assertEquals("intero", SpeechText.pick(CmdResult("1", true, "intero", 0), "coda"))

    @Test fun senzaRispostaBuonaSiLeggeIlRipiego() {
        assertEquals("coda", SpeechText.pick(null, "coda"))
        assertEquals("coda", SpeechText.pick(CmdResult("1", false, "errore", 0), "coda"))
        assertEquals("coda", SpeechText.pick(CmdResult("1", true, " ", 0), "coda"))
        assertNull(SpeechText.pick(null, " "))
    }
}
