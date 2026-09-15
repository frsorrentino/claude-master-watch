package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.Outcome
import it.pixelbox.cmwatch.rules.AnswerText.Block
import it.pixelbox.cmwatch.rules.AnswerText.Kind
import org.junit.Assert.assertEquals
import org.junit.Test

// Franz, 15/09 17:19: la risposta intera leggibile a paragrafi e ascoltabile per blocchi.
class AnswerTextTest {
    @Test fun paragrafiElenchiTitoliECodice() {
        val t = "Il preflight di release controlla.\nSeconda riga.\n\n- RL4 rosso\n- RL4b **verde**\n\n**Suite intera**\n" +
            "Non rilanciata `release.sh`.\n\n```\nbash release.sh\n```"
        assertEquals(
            listOf(
                Block(Kind.PARA, "Il preflight di release controlla. Seconda riga."),
                Block(Kind.BULLET, "RL4 rosso"),
                Block(Kind.BULLET, "RL4b verde"),
                Block(Kind.HEADING, "Suite intera"),
                Block(Kind.PARA, "Non rilanciata `release.sh`."),
                Block(Kind.CODE, "bash release.sh"),
            ),
            AnswerText.blocks(t),
        )
    }

    @Test fun titoloMarkdown() = assertEquals(listOf(Block(Kind.HEADING, "Esito")), AnswerText.blocks("## Esito"))

    @Test fun unaRigaRientrataContinuaLaVoce() =
        assertEquals(listOf(Block(Kind.BULLET, "prima parte seconda parte")), AnswerText.blocks("1. prima parte\n   seconda parte"))

    @Test fun unTestoVuotoNonHaBlocchi() = assertEquals(emptyList<Block>(), AnswerText.blocks("\n\n"))
}

// Franz, 15/09 17:18: `full` è la coda del messaggio (contratto 1.6) e sulla Scheda cominciava a metà frase.
class OutcomeTailTest {
    private val coda = "di un'altra versione non blocca.\n- Test: RL4 è stato rosso sul release.sh di prima e verde dopo la modifica. " +
        "RL4b verifica che l'«unreleased» di un'altra versione non conti.\n- Suite intera: non l'ho rilanciata. La modifica tocca " +
        "solo release.sh, che sta fuori dal plugin e lo usa soltanto"
    private val short = "release.sh non entra nello zip del plugin, quindi non serve un rilascio: il controllo vale dal prossimo release.sh."

    @Test fun unaCodaTagliataPartDallaPrimaFraseIntera() =
        assertEquals(true, OutcomeText.cardBody(Outcome(short, coda, 0))!!.startsWith("- Test: RL4"))

    @Test fun unTestoBreveNonSiTaglia() =
        assertEquals("release.sh è fuori dallo zip. Resta così.", OutcomeText.cardBody(Outcome("Fatto", "release.sh è fuori dallo zip. Resta così.", 0)))

    // Franz, 15/09 17:54: «-2 confermato sul ramo redesign;» in cima alla card di master.
    @Test fun unaCodaCheIniziaConUnNumeroMozzatoSalta() {
        val t = "-2 confermato sul ramo redesign; nessuna azione nuova da parte mia.\n- Consegne: prima la base bilingue, " +
            "poi i 17 strumenti." + " Poi il resto del lavoro che segue.".repeat(8)
        assertEquals(true, OutcomeText.cardBody(Outcome(short, t, 0))!!.startsWith("- Consegne"))
    }

    @Test fun unaCodaCheIniziaConUnaVoceDiElencoResta() {
        val t = "- Consegne: prima la base bilingue." + " Poi il resto del lavoro che segue.".repeat(8)
        assertEquals(t, OutcomeText.cardBody(Outcome(short, t, 0)))
    }

    @Test fun unaCodaCheIniziaConLaMaiuscolaResta() {
        val t = "L" + coda.drop(1)
        assertEquals(t, OutcomeText.cardBody(Outcome(short, t, 0)))
    }
}
