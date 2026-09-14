package it.pixelbox.cmwatch.wear.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.ScreenScaffoldDefaults

/**
 * Margini per lo schermo tondo (design, sezione 3: margini 5,2 % e niente testo tagliato dal cerchio):
 * lati 7 %, alto 16 %, basso 21 % dell'altezza, così la prima e l'ultima riga stanno dentro la corda del cerchio.
 */
@Composable
fun roundListPadding(sides: Float = 0.07f): PaddingValues {
    val c = LocalConfiguration.current
    val w = c.screenWidthDp.dp
    val h = c.screenHeightDp.dp
    return PaddingValues(start = w * sides, end = w * sides, top = h * 0.16f, bottom = h * 0.21f)
}

/**
 * Margini per una lista che chiude con il bottone curvo: sopra e sotto quelli della libreria, che gli lasciano il
 * posto (il nostro 21 % in basso lo spingeva fuori dallo schermo e non si vedeva), ai lati il 5,2 % del design.
 */
@Composable
fun edgeListPadding(sides: Float = 0.052f): PaddingValues {
    val base = ScreenScaffoldDefaults.contentPadding
    val w = LocalConfiguration.current.screenWidthDp.dp
    return PaddingValues(
        start = w * sides, end = w * sides,
        top = base.calculateTopPadding(), bottom = base.calculateBottomPadding(),
    )
}
