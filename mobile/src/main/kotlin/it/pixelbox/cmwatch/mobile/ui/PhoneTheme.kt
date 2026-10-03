package it.pixelbox.cmwatch.mobile.ui

import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.ui.tokens.CmColors

private val scheme = darkColorScheme(
    primary = CmColors.primary, onPrimary = CmColors.onPrimary,
    background = CmColors.bg, onBackground = CmColors.text,
    surface = CmColors.surfaceLow, onSurface = CmColors.text,
    surfaceVariant = CmColors.surface, onSurfaceVariant = CmColors.text2,
    surfaceContainer = CmColors.surface, surfaceContainerHigh = CmColors.surfaceHigh,
    outline = CmColors.line, error = CmColors.gone,
    secondaryContainer = CmColors.briefCard, onSecondaryContainer = CmColors.briefBig,
    tertiary = CmColors.waiting,
)

/** Angoli ampi e variati della scala Expressive: card grandi come le card brief, fogli e card di chi aspetta più morbidi. */
private val cmShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp), extraLarge = RoundedCornerShape(36.dp),
)

/** Gerarchia forte: numeri grandi più pesanti, come quota e contatori sul polso; titoli un gradino sopra il corpo. */
private val cmType = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontWeight = FontWeight.SemiBold),
        displayMedium = displayMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Medium),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.Medium),
    )
}

/** Negli snapshot tutto fermo allo stato finale: Paparazzi fotografa il primo fotogramma, prima di ogni animazione. */
val LocalStill = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Solo scuro, i colori dell'orologio con Material 3 Expressive, niente colori dinamici (restyling 30/09). `still` per i test. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CmPhoneTheme(still: Boolean = false, content: @Composable () -> Unit) =
    androidx.compose.runtime.CompositionLocalProvider(LocalStill provides still) {
        MaterialExpressiveTheme(
            colorScheme = scheme, motionScheme = MotionScheme.expressive(), shapes = cmShapes, typography = cmType,
            content = content,
        )
    }

/**
 * Lo switch spento visibile sulle card (Franz, 02/10 15:49: «i tasti switch sono poco visibili»): Material lo disegna con
 * `outline`, che qui è il colore delle linee, quasi uguale al fondo della card.
 */
@Composable
fun cmSwitchColors() = SwitchDefaults.colors(
    uncheckedThumbColor = CmColors.text2, uncheckedBorderColor = CmColors.text2, uncheckedTrackColor = CmColors.surfaceLow,
)

/** Tempi e curva del movimento: 250 ms e l'easing del sito; con le animazioni spente, subito lo stato finale. */
object CmMotion {
    val easing = CubicBezierEasing(0.3f, 0f, 0.2f, 1f)
    fun <T> spec(off: Boolean): FiniteAnimationSpec<T> = if (off) snap() else tween(250, easing = easing)

    /**
     * Il pannello della master che sale dalla sua barra e ci torna (Franz, 03/10 17:12): una molla senza rimbalzo, che
     * riparte dal punto in cui si trova se il gesto cambia idea a metà.
     */
    val panel = androidx.compose.animation.core.spring<androidx.compose.ui.unit.IntOffset>(
        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
        stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
        visibilityThreshold = androidx.compose.ui.unit.IntOffset.VisibilityThreshold,
    )
}

/**
 * Le aperture sul posto (▼ delle card, giorni del Registro, gruppi di passaggi; osservazioni del 03/10, transizione 3):
 * l'altezza segue il contenuto in 200 ms con la curva dell'app, quasi impercettibile; con le animazioni spente, subito.
 */
@Composable
fun Modifier.smoothSize(): Modifier =
    this.animateContentSize(if (animationsOff()) snap() else tween(200, easing = CmMotion.easing))

/** «Riduci animazioni» o animazioni di sistema a zero; negli snapshot (LocalStill) sempre «spente»: stato finale. */
@Composable
fun animationsOff(): Boolean {
    if (LocalStill.current) return true
    if (LocalInspectionMode.current) return false
    val ctx = LocalContext.current
    return remember { Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
}
