package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
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
}
