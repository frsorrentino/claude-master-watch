package it.pixelbox.cmwatch.wear.ui.ambient

import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.wear.ambient.AmbientLifecycleObserver

/** Vero mentre il quadrante è in ambient: la UI si spegne (solo badge e nome, niente colori pieni, niente animazioni). */
val LocalAmbient = compositionLocalOf { false }

/**
 * Stato ambient dell'attività, da chiamare in `onCreate` prima di `setContent`. Creato dall'interfaccia, ad attività
 * già aperta, l'osservatore non bastava: Wear OS scriveva «is not eligible for ambient lite» e si limitava a scurire
 * l'app, senza mandarle l'ambient (logcat del polso, 14/09 21:17 e 21:19).
 */
fun ComponentActivity.ambientState(): State<Boolean> {
    val state = mutableStateOf(false)
    lifecycle.addObserver(AmbientLifecycleObserver(this, object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(details: AmbientLifecycleObserver.AmbientDetails) { state.value = true }
        override fun onUpdateAmbient() {}
        override fun onExitAmbient() { state.value = false }
    }))
    return state
}

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
