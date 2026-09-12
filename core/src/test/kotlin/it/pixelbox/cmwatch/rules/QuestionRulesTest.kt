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

    @Test fun optionLabelKeepsNumber() = assertEquals("1 · yes", QuestionRules.optionLabel(q.options[0]))

    @Test fun firstOptionIsTheOnlyPrimary() {
        assertTrue(QuestionRules.isPrimary(0)); assertFalse(QuestionRules.isPrimary(1))
    }
}
