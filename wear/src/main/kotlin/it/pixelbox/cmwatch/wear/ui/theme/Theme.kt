package it.pixelbox.cmwatch.wear.ui.theme

import it.pixelbox.cmwatch.ui.tokens.CmColors

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


/**
 * Il colore segnala ciò che chiede attenzione, il resto resta neutro (review UX, 13/09): ambra a chi aspetta te,
 * cobalto a chi lavora, grigio chiaro alle ferme, rosso desaturato alle chiuse. Quattro tinte sature insieme
 * appiattivano la gerarchia e il verde acceso su «ferma» attirava quanto l'ambra di «aspetta te».
 */
fun stateColor(s: SessionState, fresh: Boolean = true): Color = if (!fresh) CmColors.stale else when (s) {
    SessionState.WAITING -> CmColors.waiting
    SessionState.BUSY, SessionState.AWAITING -> CmColors.accent
    SessionState.IDLE -> CmColors.text2
    SessionState.GONE -> CmColors.goneDim
}

private val scheme = ColorScheme(
    primary = CmColors.primary, primaryDim = Color(0xFFA8C7FA), primaryContainer = CmColors.primary,
    onPrimary = CmColors.onPrimary, onPrimaryContainer = CmColors.onPrimary,
    secondary = CmColors.text2, secondaryDim = CmColors.text2, secondaryContainer = CmColors.surface,
    onSecondary = CmColors.bg, onSecondaryContainer = CmColors.text,
    tertiary = CmColors.busy, tertiaryDim = CmColors.busy, tertiaryContainer = CmColors.surface,
    onTertiary = CmColors.bg, onTertiaryContainer = CmColors.text,
    surfaceContainerLow = CmColors.surfaceLow, surfaceContainer = CmColors.surface, surfaceContainerHigh = CmColors.surfaceHigh,
    onSurface = CmColors.text, onSurfaceVariant = CmColors.text2,
    outline = CmColors.line, outlineVariant = CmColors.line,
    background = CmColors.bg, onBackground = CmColors.text,
    error = CmColors.gone, errorDim = CmColors.gone, errorContainer = CmColors.gone,
    onError = CmColors.text, onErrorContainer = CmColors.text,
)

val Mono = FontFamily.Monospace

/**
 * Numero grande della card in stile brief: 26 sp. Misurato sulla schermata vera (altezza delle maiuscole 18 dp a
 * densità 2,0); i token `numeral*` di Wear M3 sono molto più grandi e sfondavano la card.
 */
val BriefNumber = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Medium)

/** Nomi di sessione e terminale: mono 14 sp (design, sezione 3). */
val MonoStyle = TextStyle(fontSize = 14.sp, fontFamily = Mono, fontWeight = FontWeight.Medium)

/** Nome nella lista delle sessioni: mono 13 sp, così «claude-master» ci sta intero accanto al badge e all'età. */
val SessionNameStyle = TextStyle(fontSize = 13.sp, fontFamily = Mono, fontWeight = FontWeight.Medium)

/** Terminale: mono 13 sp, così una riga di comando sta in più caratteri e il tondo ne taglia meno (13/09 17:40). */
val TerminalStyle = TextStyle(fontSize = 13.sp, fontFamily = Mono)

/**
 * Scala più vicina a quella delle app di sistema del Pixel Watch (review UX, 13/09): la frase protagonista, cioè
 * l'esito, passa da 20 a 26 sp; titoli di riga 18, corpo 16, secondario 15, minuto 13. I nomi restano in mono e
 * fuori da questa scala, perché nella lista devono starci interi.
 */
private val typography = Typography(
    displaySmall = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 13.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    bodySmall = TextStyle(fontSize = 15.sp),
)

@Composable
fun CmTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = scheme, typography = typography, content = content)
