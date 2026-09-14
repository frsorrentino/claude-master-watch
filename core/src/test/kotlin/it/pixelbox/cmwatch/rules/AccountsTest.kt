package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.contract.ContractJson
import org.junit.Assert.*
import org.junit.Test

/**
 * Contratto 1.8 (Franz, 14/09 22:55): quale account è personale lo dice il tipo, non il nome. Gli account possono
 * chiamarsi in qualunque modo (beta pubblica S08); senza il tipo, un relay vecchio, si ripiega sul nome «personale».
 */
class AccountsTest {
    @Test fun ilTipoVinceSulNome() {
        assertTrue(Accounts.personal("home", "personal"))
        assertFalse(Accounts.personal("personale", "work"))
    }

    @Test fun senzaTipoSiRipiegaSulNome() {
        assertTrue(Accounts.personal("personale", null))
        assertTrue(Accounts.personal("Personale", null))
        assertFalse(Accounts.personal("agenzia", null))
    }

    @Test fun unTipoSconosciutoNonEPersonale() = assertFalse(Accounts.personal("home", "family"))

    @Test fun ilContrattoPortaIlTipoDiSessioneEQuota() {
        val s = ContractJson.decodeState(
            """{"v":1,"ts":1,"host":"h","sessions":[{"id":"a","name":"x","account":"home","account_kind":"personal","project":"p","state":"idle","since":1}],
               "quota":{"home":{"h5":10,"kind":"personal"},"office":{"h5":20,"kind":"work"}}}""",
        )
        assertTrue(Accounts.isPersonal(s.sessions[0]))
        assertTrue(Accounts.isPersonalQuota("home", s.quota.getValue("home")))
        assertFalse(Accounts.isPersonalQuota("office", s.quota.getValue("office")))
    }
}
