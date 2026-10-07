package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.Option
import it.pixelbox.cmwatch.contract.QuestionKind
import it.pixelbox.cmwatch.contract.Tier
import org.junit.Assert.*
import org.junit.Test

class QuestionRulesTest {
    private val q = ContractJson.decodeState(Fixtures.stateQuestion).sessions[0].question!!

    @Test fun highNeedsLongPress() {
        assertTrue(QuestionRules.needsLongPress(Tier.HIGH))
        assertFalse(QuestionRules.needsLongPress(Tier.MEDIUM)); assertFalse(QuestionRules.needsLongPress(Tier.LOW))
    }

    /** Dal vivo 30/09: un permesso senza opzioni lette (strumento Agent) non ha «non chiedere più», e il relay rifiuta allow_all. */
    @Test fun allowAllHiddenWhenThePermissionHasNoOptions() =
        assertFalse(QuestionRules.allowAllVisible(q.copy(kind = QuestionKind.PERMISSION, options = emptyList())))

    @Test fun allowAllOnlyForPermissionsBelowHigh() {
        assertFalse(QuestionRules.allowAllVisible(q))                                          // kind = ask
        assertTrue(QuestionRules.allowAllVisible(q.copy(kind = QuestionKind.PERMISSION)))
        assertFalse(QuestionRules.allowAllVisible(q.copy(kind = QuestionKind.PERMISSION, tier = Tier.HIGH)))
    }

    // Franz, 15/09 16:13: nell'etichetta arrivava anche l'anteprima dell'opzione, un riquadro disegnato a caratteri.
    @Test fun optionLabelDropsThePreviewBox() {
        assertEquals("1 · Due binari (Consigliata)", QuestionRules.optionLabel(it.pixelbox.cmwatch.contract.Option(1, "Due binari (Consigliata)\n┌──────────┐\n│ FRANCESCO │\n└──────────┘")))
        assertEquals("3 · Prima l'e-commerce", QuestionRules.optionLabel(it.pixelbox.cmwatch.contract.Option(3, "Prima l'e-commerce │           │")))
        assertEquals("2 · Prima il builder", QuestionRules.optionLabel(it.pixelbox.cmwatch.contract.Option(2, "┌───┐\nPrima il builder")))
    }

    @Test fun optionLabelKeepsNumber() =assertEquals("1 · yes", QuestionRules.optionLabel(q.options[0]))

    @Test fun firstOptionIsTheOnlyPrimary() {
        assertTrue(QuestionRules.isPrimary(0)); assertFalse(QuestionRules.isPrimary(1))
    }

    // Righe B del riepilogo (Franz, 03/10 15:18): opzioni brevi su una riga in parti uguali, le lunghe una sotto l'altra.
    @Test fun shortOptionsShareOneRow() =
        assertTrue(QuestionRules.inline(listOf(Option(1, "yes"), Option(2, "no"))))
    @Test fun longOptionsStack() =
        assertFalse(QuestionRules.inline(listOf(Option(1, "Yes, and don't ask again for this command"), Option(2, "No"))))
    @Test fun fourOptionsStack() =
        assertFalse(QuestionRules.inline((1..4).map { Option(it, "o$it") }))
    @Test fun oneOptionStacks() = assertFalse(QuestionRules.inline(listOf(Option(1, "ok"))))

    // Franz, 07/10 18:01: il relay mandava «-: -» come testo e sull'orologio la domanda sembrava vuota.
    @Test fun aTextWithoutWordsGivesWayToTheFallback() {
        for (t in listOf("-: -", "", "   ", "──── ", "?")) assertEquals(t, "Ti chiede di scegliere", QuestionRules.shownText(t, "Ti chiede di scegliere"))
    }

    @Test fun aRealTextStays() {
        for (t in listOf("Deploy now?", "Procedo con il push?", "1 o 2?")) assertEquals(t, t, QuestionRules.shownText(t, "x"))
    }
}
