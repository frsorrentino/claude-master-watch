package it.pixelbox.cmwatch.wear.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnItemScope
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

/**
 * Testi, intestazioni e righe senza superficie si deformano come le card (S07): vicino al bordo tondo la libreria li
 * stringe invece di lasciarli tagliare dal cerchio. I margini delle liste sono quelli di default di `ScreenScaffold`
 * (5,2 % ai lati, 10 % sopra e sotto, più lo spazio del bottone curvo), non più percentuali calcolate da noi.
 */
fun Modifier.morph(scope: TransformingLazyColumnItemScope, spec: TransformationSpec): Modifier = with(scope) {
    this@morph.transformedHeight(scope, spec).graphicsLayer { with(spec) { applyContainerTransformation(scrollProgress) } }
}
