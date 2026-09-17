package it.pixelbox.cmwatch.wear

/**
 * Nomi e testi della demo per i video promozionali (Franz, 16/09 18:11: «nomi meno banali e più verosimili», «più righe»).
 * Le fixture del contratto restano identiche byte per byte a quelle del relay, che le confronta nei suoi test: qui si
 * cambiano solo nel momento in cui l'app le carica per la demo. Le frasi più lunghe vengono prima delle più corte che le
 * contengono («Deploy ready, waiting…» prima di «Deploy ready»).
 */
object DemoText {
    private val sostituzioni = listOf(
        "Deploy ready, waiting for the client's ok. Deploy now?" to
            "Refund endpoint is ready and staging checks passed. Deploy version 2.8.0 to production now?",
        "Esito: migrations 008-011 applied, tests green.\\nThe test seeds and the admin page are still to review." to
            "Applied migrations 008-011 for saved carts and gift cards; all 214 tests are green.\\nThe seed data and the new orders admin page are still to review, and the gift card flow needs a manual check on staging.",
        "Esito: README rewritten with the three sections asked for." to
            "Rewrote the post on edge caching with the three sections you asked for, added the benchmark table and fixed three broken links.",
        "Esito: seeds and admin page reviewed, 42 tests green." to
            "Reviewed the seed data and the orders admin page; 214 tests green and the gift card flow checked on staging.",
        "Migrations 008-011 applied, tests green" to "Applied migrations 008-011 for saved carts and gift cards; all 214 tests green",
        "Review the seeds and the admin page" to "Review the seed data and the new orders admin page",
        "README rewritten" to "Rewrote the post on edge caching, added the benchmark table and fixed three broken links",
        "Run the test suite" to "Run the checkout test suite after the Stripe webhook refactor",
        "Wait for the go" to "Wait for the client's go, then deploy 2.8.0",
        "Pick up the pricing page" to "Pick up the pricing page redesign for the App Store listing",
        "Deploy ready" to "Refund endpoint ready",
        "ledger-api" to "payments-api",
        "atlas-shop" to "storefront",
        "field-notes" to "blog",
        "orbit-docs" to "ios-app",
        "crostini-demo" to "dev-laptop",
        // La quota di lavoro della fixture è «stale» e senza finestra di 5 ore: nel video sembrava un errore (17/09 02:30).
        "\"h5\": null,\n      \"w7\": 75," to "\"h5\": 38,\n      \"w7\": 75,",
        "\"stale\": true,\n      \"kind\": \"work\"" to "\"stale\": false,\n      \"kind\": \"work\"",
    )

    fun dress(json: String): String = sostituzioni.fold(json) { testo, (da, a) -> testo.replace(da, a) }
}
