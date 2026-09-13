package it.pixelbox.cmwatch.wear.ui.ambient

import android.app.Activity
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.ambient.AmbientLifecycleObserver

/** Vero mentre il quadrante è in ambient: la UI si spegne (solo badge e nome, niente colori pieni, niente animazioni). */
@Composable
fun rememberAmbient(): Boolean {
    val ctx = LocalContext.current
    val activity = ctx as? Activity ?: return false
    val owner = LocalLifecycleOwner.current
    var ambient by remember { mutableStateOf(false) }
    DisposableEffect(activity, owner) {
        val observer = AmbientLifecycleObserver(activity, object : AmbientLifecycleObserver.AmbientLifecycleCallback {
            override fun onEnterAmbient(details: AmbientLifecycleObserver.AmbientDetails) { ambient = true }
            override fun onUpdateAmbient() {}
            override fun onExitAmbient() { ambient = false }
        })
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return ambient
}

/** Il sistema chiede di non animare (Reduce Motion / scala animazioni a zero). */
@Composable
fun animationsOff(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) {
        runCatching { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
}
