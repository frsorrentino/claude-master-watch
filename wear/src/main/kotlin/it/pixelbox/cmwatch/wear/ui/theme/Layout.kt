package it.pixelbox.cmwatch.wear.ui.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Margini per lo schermo tondo (design, sezione 3: margini 5,2 % e niente testo tagliato dal cerchio):
 * lati 7 %, alto 16 %, basso 21 % dell'altezza, così la prima e l'ultima riga stanno dentro la corda del cerchio.
 */
@Composable
fun roundListPadding(): PaddingValues {
    val c = LocalConfiguration.current
    val w = c.screenWidthDp.dp
    val h = c.screenHeightDp.dp
    return PaddingValues(start = w * 0.07f, end = w * 0.07f, top = h * 0.16f, bottom = h * 0.21f)
}
