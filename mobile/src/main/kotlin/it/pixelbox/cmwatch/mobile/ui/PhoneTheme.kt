package it.pixelbox.cmwatch.mobile.ui

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import it.pixelbox.cmwatch.ui.tokens.CmColors

private val scheme = darkColorScheme(
    primary = CmColors.primary, onPrimary = CmColors.onPrimary,
    background = CmColors.bg, onBackground = CmColors.text,
    surface = CmColors.surfaceLow, onSurface = CmColors.text,
    surfaceVariant = CmColors.surface, onSurfaceVariant = CmColors.text2,
    surfaceContainer = CmColors.surface, surfaceContainerHigh = CmColors.surfaceHigh,
    outline = CmColors.line, error = CmColors.gone,
)

/** Solo scuro, i colori dell'orologio, niente colori dinamici (design 24/09, «Aspetto e movimento»). */
@Composable
fun CmPhoneTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = scheme, content = content)

/** Tempi e curva del movimento: 250 ms e l'easing del sito; con le animazioni spente, subito lo stato finale. */
object CmMotion {
    val easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)
    fun <T> spec(off: Boolean): FiniteAnimationSpec<T> = if (off) snap() else tween(250, easing = easing)
}

/** «Riduci animazioni» o animazioni di sistema a zero. Negli snapshot vale sempre «accese». */
@Composable
fun animationsOff(): Boolean {
    if (LocalInspectionMode.current) return false
    val ctx = LocalContext.current
    return remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
