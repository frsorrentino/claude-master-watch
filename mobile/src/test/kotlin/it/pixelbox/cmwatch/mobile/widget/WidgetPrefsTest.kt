package it.pixelbox.cmwatch.mobile.widget

import androidx.datastore.preferences.core.mutablePreferencesOf
import it.pixelbox.cmwatch.rules.WidgetModel
import it.pixelbox.cmwatch.rules.WidgetModel.Metric
import it.pixelbox.cmwatch.rules.WidgetModel.Mode
import org.junit.Assert.*
import org.junit.Test

/**
 * La configurazione vive nello stato Glance del widget (Franz, 01/10 12:37: le scelte si applicavano dopo tempo o più
 * salvataggi, perché un file di preferenze a parte non fa ridisegnare il widget). Qui il giro scrivi → leggi.
 */
class WidgetPrefsTest {
    @Test fun emptyStateIsTheDefault() = assertEquals(WidgetPrefs.DEFAULT, WidgetPrefs.read(mutablePreferencesOf()))

    @Test fun configRoundTrip() {
        val c = WidgetModel.Config(Mode.SESSION, "atlas-shop", listOf(Metric.OUTCOME, Metric.TURN_AGE), 60, 12, mono = false)
        val p = mutablePreferencesOf()
        WidgetPrefs.write(p, c)
        assertEquals(c, WidgetPrefs.read(p))
    }

    /** Il monocromo si spegne davvero, e una sessione «la seguita» torna a null. */
    @Test fun switchingBackClearsMonoAndTarget() {
        val p = mutablePreferencesOf()
        WidgetPrefs.write(p, WidgetPrefs.DEFAULT.copy(mode = Mode.ACCOUNT, target = "work", mono = true))
        WidgetPrefs.write(p, WidgetPrefs.DEFAULT.copy(mode = Mode.SESSION, target = null, mono = false))
        val back = WidgetPrefs.read(p)
        assertFalse(back.mono); assertNull(back.target); assertEquals(Mode.SESSION, back.mode)
    }

    /** Monocromo acceso di partenza (Franz, 01/10 23:16), anche per un widget senza scelte salvate. */
    @Test fun monoIsOnByDefault() {
        assertTrue(WidgetPrefs.DEFAULT.mono)
        assertTrue(WidgetPrefs.read(androidx.datastore.preferences.core.mutablePreferencesOf()).mono)
    }
}
