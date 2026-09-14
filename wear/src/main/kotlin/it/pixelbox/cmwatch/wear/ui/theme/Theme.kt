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
    /** Tre gradini di superficie, non uno: sfondo, riga, card. Senza il gradino la profondità non si vede. */
    val surfaceLow = Color(0xFF1B1F26)
    val surface = Color(0xFF23272E)   // più chiaro (Franz, 12/09 20:03): come i tasti del selettore app di Wear OS
    val surfaceHigh = Color(0xFF292F3A)   // card in stile brief, misurata sui suoi fotogrammi
    val line = Color(0xFF2A2E35)
    val text = Color(0xFFF2F4F7)
    val text2 = Color(0xFF9AA3B2)
    val accent = Color(0xFF4C7DFF)
    val accentPressed = Color(0xFF3457D5)
    val waiting = Color(0xFFFFB020)
    // Giallino della sessione seguita, «come se fosse accesa» (Franz, 14/09 16:27): più chiaro dell'ambra di attesa.
    val followed = Color(0xFFFFE08A)
    val busy = Color(0xFF7FA1FF)
    val idle = Color(0xFF34C759)
    val gone = Color(0xFFFF453A)
    val goneDim = Color(0xFFC2554D)   // rosso desaturato: una sessione chiusa non deve urlare
    // 5:1 sulla superficie: il grigio di prima stava a 3:1 e al sole spariva (review UX, 13/09).
    val stale = Color(0xFF98A2B3)
    val accountAgenzia = Color(0xFFE53935)
    val accountPersonale = Color(0xFF43A047)

    // Stile del «brief mattutino» di Wear OS, misurato sui fotogrammi della sua schermata (13/09 16:25).
    val briefCard = surfaceHigh
    val briefLabel = Color(0xFFBCE4C7)
    val briefBig = Color(0xFFF4F4F4)
    val briefSecondary = Color(0xFFBCC0CB)
    val briefRing = Color(0xFF8BB4F7)
    val briefTrack = Color(0xFF455165)
    val briefChip = Color(0xFF7DCCFB)
    val briefChipInk = Color(0xFF001C33)
    val briefGood = Color(0xFF65C581)
    val briefGoodInk = Color(0xFF072510)
    val briefWarn = Color(0xFFFFC46B)
    val briefWarnInk = Color(0xFF2A1A00)
    val briefAlert = Color(0xFFF2B8B5)
    val briefAlertInk = Color(0xFF5F1412)
    val briefAlertRing = Color(0xFFE5736B)
}

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
    primary = CmColors.accent, primaryDim = CmColors.accentPressed, primaryContainer = CmColors.accent,
    onPrimary = CmColors.text, onPrimaryContainer = CmColors.text,
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
