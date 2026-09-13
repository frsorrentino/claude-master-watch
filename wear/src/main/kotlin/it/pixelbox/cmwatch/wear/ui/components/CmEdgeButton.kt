package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/**
 * L'azione principale della schermata, curva in fondo come nelle app di Wear OS 6 e come già nella nostra tile
 * (review UX, 13/09, scelta da Franz): segue il bordo, resta sotto il pollice e non consuma una riga della lista,
 * che prima era una pila di bottoni larghi tutti uguali.
 */
@Composable
fun CmEdgeButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    EdgeButton(
        onClick = onClick,
        enabled = enabled,
        buttonSize = EdgeButtonSize.Medium,
        colors = ButtonDefaults.buttonColors(containerColor = CmColors.accent, contentColor = CmColors.text),
    ) {
        Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
