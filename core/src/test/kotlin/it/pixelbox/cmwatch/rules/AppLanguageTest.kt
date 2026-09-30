package it.pixelbox.cmwatch.rules

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test fun systemFollowsThePhone() =
        assertEquals(Locale.GERMANY, AppLanguage.voiceLocale(AppLanguage.Choice.SYSTEM, Locale.GERMANY))

    @Test fun chosenLanguageDrivesTheVoice() =
        assertEquals("it", AppLanguage.voiceLocale(AppLanguage.Choice.ITALIAN, Locale.US).language)

    @Test fun tagsRoundTrip() {
        assertEquals(AppLanguage.Choice.ENGLISH, AppLanguage.fromTags("en"))
        assertEquals(AppLanguage.Choice.SYSTEM, AppLanguage.fromTags(""))
        assertEquals(AppLanguage.Choice.SYSTEM, AppLanguage.fromTags("fr"))
    }
}
