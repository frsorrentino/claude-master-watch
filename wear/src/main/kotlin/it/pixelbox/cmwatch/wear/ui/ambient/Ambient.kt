package it.pixelbox.cmwatch.wear.ui.ambient

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Vero mentre il quadrante è in ambient: la UI si spegne (solo badge e nome, niente colori pieni, niente animazioni).
 * Lo fornisce `MainActivity` da `rememberAmbientModeManager()` di Wear Compose.
 */
val LocalAmbient = compositionLocalOf { false }

/** Legge lo stato ambient dell'attività. */
@Composable
fun rememberAmbient(): Boolean = LocalAmbient.current

/** Il sistema chiede di non animare (Reduce Motion / scala animazioni a zero). */
@Composable
fun animationsOff(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
}
