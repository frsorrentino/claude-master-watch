package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeakableNameTest {
    @Test fun specExamples() {
        assertEquals("atlas shop", SpeakableName.of("pix-atlas-shop-it"))
        assertEquals("ledger api", SpeakableName.of("ledger-api"))
        assertEquals("fccrotone", SpeakableName.of("pix-fccrotone-it"))
    }

    @Test fun everyFinalDomainGoes() {
        val table = mapOf(
            "shop-example-com" to "shop example", "news-eu" to "news", "tools-net" to "tools",
            "site-org" to "site", "phone-app" to "phone", "orbit-docs" to "orbit docs",
        )
        for ((name, spoken) in table) assertEquals(name, spoken, SpeakableName.of(name))
    }

    @Test fun caseAndOtherSeparators() {
        assertEquals("Atlas", SpeakableName.of("Pix-Atlas-IT"))
        assertEquals("atlas shop", SpeakableName.of("atlas_shop.it"))
    }

    // Un nome fatto solo di prefisso e dominio non diventa vuoto: la voce direbbe il nulla.
    @Test fun neverEmpty() {
        assertEquals("it", SpeakableName.of("pix-it"))
        assertEquals("it", SpeakableName.of("it"))
        assertEquals("pix", SpeakableName.of("pix"))
        assertEquals("", SpeakableName.of(""))
    }

    @Test fun prefixOnlyAtTheStart() {
        assertEquals("app pix", SpeakableName.of("app-pix"))
    }
}
