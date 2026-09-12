package it.pixelbox.cmwatch.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import it.pixelbox.cmwatch.contract.SessionState

/** Token del design (sezione 3): tema scuro unico, cobalto solo su bottone pieno, scroll bar, chip «seguita», anello quota. */
object CmColors {
    val bg = Color(0xFF000000)
    val surface = Color(0xFF121417)
    val line = Color(0xFF2A2E35)
    val text = Color(0xFFF2F4F7)
    val text2 = Color(0xFF9AA3B2)
    val accent = Color(0xFF4C7DFF)
    val accentPressed = Color(0xFF3457D5)
    val waiting = Color(0xFFFFB020)
    val busy = Color(0xFF7FA1FF)
    val idle = Color(0xFF34C759)
    val gone = Color(0xFFFF453A)
    val stale = Color(0xFF6B7280)
    val accountAgenzia = Color(0xFFE53935)
    val accountPersonale = Color(0xFF43A047)
}

fun stateColor(s: SessionState, fresh: Boolean = true): Color = if (!fresh) CmColors.stale else when (s) {
    SessionState.WAITING -> CmColors.waiting
    SessionState.BUSY -> CmColors.busy
    SessionState.IDLE, SessionState.AWAITING -> CmColors.idle
    SessionState.GONE -> CmColors.gone
}

private val scheme = ColorScheme(
    primary = CmColors.accent, primaryDim = CmColors.accentPressed, primaryContainer = CmColors.accent,
    onPrimary = CmColors.text, onPrimaryContainer = CmColors.text,
    secondary = CmColors.text2, secondaryDim = CmColors.text2, secondaryContainer = CmColors.surface,
    onSecondary = CmColors.bg, onSecondaryContainer = CmColors.text,
    tertiary = CmColors.busy, tertiaryDim = CmColors.busy, tertiaryContainer = CmColors.surface,
    onTertiary = CmColors.bg, onTertiaryContainer = CmColors.text,
    surfaceContainerLow = CmColors.surface, surfaceContainer = CmColors.surface, surfaceContainerHigh = CmColors.surface,
    onSurface = CmColors.text, onSurfaceVariant = CmColors.text2,
    outline = CmColors.line, outlineVariant = CmColors.line,
    background = CmColors.bg, onBackground = CmColors.text,
    error = CmColors.gone, errorDim = CmColors.gone, errorContainer = CmColors.gone,
    onError = CmColors.text, onErrorContainer = CmColors.text,
)

val Mono = FontFamily.Monospace

private val typography = Typography(
    displaySmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, fontFamily = Mono),
    labelSmall = TextStyle(fontSize = 13.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    bodySmall = TextStyle(fontSize = 13.sp),
)

@Composable
fun CmTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
