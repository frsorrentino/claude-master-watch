package it.pixelbox.cmwatch.rules

import it.pixelbox.cmwatch.Fixtures
import it.pixelbox.cmwatch.contract.ContractJson
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.rules.LiveRoute.Intent
import it.pixelbox.cmwatch.rules.LiveRoute.Route
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveRouteTest {
    // state-1: ledger-api, atlas-shop, field-notes e orbit-docs (chiusa); una richiesta di ok, atlas-release-2-4
    private val q = ContractJson.decodeState(Fixtures.stateQuestion)

    private fun with(vararg names: String): State =
        q.copy(sessions = q.sessions + names.map { q.sessions.first().copy(name = it, question = null) })

    @Test fun instructions() {
        val table = listOf(
            "di' a ledger api: aspetta il cliente" to Route.Tell("ledger-api", "aspetta il cliente"),
            "Dì a Ledger API aspetta il cliente" to Route.Tell("ledger-api", "aspetta il cliente"),
            "di’ a atlas shop di lanciare i test" to Route.Tell("atlas-shop", "di lanciare i test"),
            "di a atlas shop, lancia i test" to Route.Tell("atlas-shop", "lancia i test"),
            "chiedi ad atlas shop se ha finito" to Route.Tell("atlas-shop", "se ha finito"),
            "manda a field notes il riepilogo" to Route.Tell("field-notes", "il riepilogo"),
        )
        for ((text, route) in table) assertEquals(text, route, LiveRoute.parse(text, q))
    }

    // Il nome si risolve prima per uguaglianza, poi per prefisso, poi per contenuto.
    @Test fun nameResolution() {
        assertEquals(Route.Tell("atlas-shop", "lancia i test"), LiveRoute.parse("scrivi a atlas lancia i test", q))
        assertEquals(Route.Tell("field-notes", "se ha finito"), LiveRoute.parse("chiedi a notes se ha finito", q))
        val both = with("pix-atlas-it")
        assertEquals(Route.Tell("pix-atlas-it", "fai il deploy"), LiveRoute.parse("di' a atlas fai il deploy", both))
        assertEquals(Route.Tell("atlas-shop", "fai il deploy"), LiveRoute.parse("di' a atlas shop fai il deploy", both))
    }

    @Test fun ambiguousOrMissingNames() {
        assertEquals(
            Route.Pick(Intent.TELL, listOf("atlas-shop", "atlas-blog"), "fai il deploy"),
            LiveRoute.parse("di' a atlas fai il deploy", with("atlas-blog")),
        )
        assertEquals(Route.NotFound("zeta"), LiveRoute.parse("di' a zeta fai il deploy", q))
        // una sessione chiusa non riceve istruzioni
        assertEquals(Route.NotFound("orbit"), LiveRoute.parse("di' a orbit docs riprendi", q))
    }

    @Test fun anInstructionWithoutTextAsksForIt() {
        assertEquals(Route.AskText("ledger-api"), LiveRoute.parse("manda a ledger api", q))
        assertEquals(Route.AskText("ledger-api"), LiveRoute.parse("di' a ledger api:", q))
    }

    @Test fun statusQuestions() {
        assertEquals(Route.Status("atlas-shop"), LiveRoute.parse("com'è messa atlas shop?", q))
        assertEquals(Route.Status("ledger-api"), LiveRoute.parse("A che punto è ledger", q))
        assertEquals(Route.Status("orbit-docs"), LiveRoute.parse("come va orbit docs", q))
        assertEquals(Route.Waiting, LiveRoute.parse("Chi mi aspetta?", q))
        assertEquals(Route.Waiting, LiveRoute.parse("chi aspetta me", q))
        assertEquals(Route.Quota, LiveRoute.parse("quanta quota mi resta", q))
        assertEquals(Route.Round, LiveRoute.parse("giro completo", q))
        assertEquals(Route.Round, LiveRoute.parse("Fammi il giro", q))
        assertEquals(Route.Pick(Intent.STATUS, listOf("atlas-shop", "atlas-blog")), LiveRoute.parse("come va atlas", with("atlas-blog")))
    }

    // «approva» apre soltanto la doppia conferma: la rotta porta il compito, non un comando.
    @Test fun approvals() {
        assertEquals(Route.Approve("atlas-release-2-4"), LiveRoute.parse("approva release 2.4", q))
        assertEquals(Route.Approve("atlas-release-2-4"), LiveRoute.parse("Approva la release 2.4", q))
        assertEquals(Route.Approve("atlas-release-2-4"), LiveRoute.parse("approva atlas release", q))
        assertEquals(Route.Approve("atlas-release-2-4"), LiveRoute.parse("approva", q))
        assertEquals(Route.NotFound("il deploy di marte"), LiveRoute.parse("approva il deploy di marte", q))
        assertEquals(Route.NotFound(""), LiveRoute.parse("approva", q.copy(approvals = emptyList())))
        val two = q.copy(approvals = q.approvals + q.approvals.first().copy(task = "blog-post", title = "Post on the blog"))
        assertEquals(Route.Pick(Intent.APPROVE, listOf("atlas-release-2-4", "blog-post")), LiveRoute.parse("approva", two))
    }

    @Test fun everythingElseGoesToTheMaster() {
        assertEquals(Route.Master("perché il test di ieri è fallito?"), LiveRoute.parse("perché il test di ieri è fallito?", q))
        assertEquals(Route.Master("come va"), LiveRoute.parse("come va", q))
        // senza una sessione dopo «come va» la regola dello stato non si applica: «la» non cerca dentro «atlas shop»
        assertEquals(Route.Master("come va la quota?"), LiveRoute.parse("come va la quota?", q))
        assertEquals(Route.Master("dimmi una cosa"), LiveRoute.parse("dimmi una cosa", q))
    }

    @Test fun emptyTranscription() {
        assertEquals(Route.Empty, LiveRoute.parse("", q))
        assertEquals(Route.Empty, LiveRoute.parse("   ", q))
    }
}
