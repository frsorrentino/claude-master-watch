package it.pixelbox.cmwatch.wear.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.PaddingDefaults
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * Testi, intestazioni e righe senza superficie si deformano come le card (S07): vicino al bordo tondo la libreria li
 * stringe invece di lasciarli tagliare dal cerchio. I margini delle liste sono quelli di default di `ScreenScaffold`
 * (5,2 % ai lati, 10 % sopra e sotto, più lo spazio del bottone curvo), non più percentuali calcolate da noi.
 * Dentro, il margine di contenuto della libreria: il testo nudo parte dove parte il testo delle card, non dal bordo
 * della card (negli snapshot del 15/09 00:50 le ultime righe di Esito e Timeline perdevano ancora la prima lettera).
 */
@Composable
fun Modifier.morph(scope: TransformingLazyColumnItemScope, spec: TransformationSpec): Modifier = with(scope) {
    this@morph.transformedHeight(scope, spec)
        .graphicsLayer { with(spec) { applyContainerTransformation(scrollProgress) } }
        .padding(horizontal = PaddingDefaults.horizontalContentPadding())
}
