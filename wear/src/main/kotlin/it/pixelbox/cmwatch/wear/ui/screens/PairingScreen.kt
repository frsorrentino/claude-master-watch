package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.ui.tokens.CmColors
import it.pixelbox.cmwatch.wear.ui.theme.morph

/**
 * Accoppiamento (design 24/09): dal telefono, che legge il QR di `relay pair`; il codice a 6 cifre solo se la build ha la
 * configurazione dentro. In fondo la Demo, per chi non ha un PC (Franz, 26/09 18:09).
 */
@Composable
fun PairingScreen(status: PairingStatus, withCode: Boolean, onOpenPhone: () -> Unit, onEnterCode: () -> Unit, onRetry: () -> Unit, onDemo: () -> Unit = {}) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.pairing_title)) } }
            item {
                val text = when (status) {
                    PairingStatus.Idle -> stringResource(if (withCode) R.string.pairing_hint_both else R.string.pairing_hint_phone)
                    PairingStatus.Working -> stringResource(R.string.pairing_working)
                    is PairingStatus.Done -> stringResource(R.string.pairing_done, status.host)
                    is PairingStatus.Failed -> stringResource(R.string.pairing_failed)
                }
                val color = if (status is PairingStatus.Failed) CmColors.gone else CmColors.text2
                Text(text, style = MaterialTheme.typography.bodyMedium, color = color, modifier = Modifier.fillMaxWidth().morph(this, spec))
            }
            item {
                if (status is PairingStatus.Failed) {
                    WideButton(stringResource(R.string.question_retry), onClick = onRetry, primary = true, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
                } else {
                    WideButton(stringResource(R.string.pairing_open_phone), onClick = onOpenPhone, primary = true, enabled = status !is PairingStatus.Working, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
                }
            }
            if (withCode && status !is PairingStatus.Failed) item {
                WideButton(stringResource(R.string.pairing_enter_code), onClick = onEnterCode, primary = false, enabled = status !is PairingStatus.Working, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
            if (status is PairingStatus.Idle) item {
                WideButton(stringResource(R.string.pairing_demo), onClick = onDemo, primary = false, transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec))
            }
        }
    }
}

sealed class PairingStatus {
    data object Idle : PairingStatus()
    data object Working : PairingStatus()
    data class Done(val host: String) : PairingStatus()
    data class Failed(val reason: String) : PairingStatus()
}
