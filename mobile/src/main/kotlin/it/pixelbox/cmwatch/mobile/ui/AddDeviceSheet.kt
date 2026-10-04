package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.nayuki.qrcodegen.QrCode
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.PairAddText
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Contratto 1.31: l'invito per un dispositivo in più, chiesto dal telefono al relay. */
sealed interface AddDeviceUi {
    data object Asking : AddDeviceUi
    /** `qr` = il testo del QR della 1.30, null se il PC non ha i dati dell'app Firebase; `code` a 6 cifre; `exp` epoch s. */
    data class Offer(val qr: String?, val code: String, val exp: Long) : AddDeviceUi
    data class Refused(val refusal: PairAddText.Refusal, val reason: String) : AddDeviceUi
    /** Nessuna risposta dal PC nel tempo di una lettura. */
    data object Lost : AddDeviceUi
}

/**
 * Aggiungere un dispositivo dal telefono, senza PC (Franz, 04/10 13:47): il QR da inquadrare col dispositivo nuovo, il
 * codice, quanto resta (5 minuti), poi «Fatto». Scaduto, se ne chiede un altro. `now` fermo nei provini.
 */
@Composable
fun AddDeviceSheet(ui: AddDeviceUi, onRetry: () -> Unit, onDone: () -> Unit, now: Long? = null) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.add_device), style = MaterialTheme.typography.titleLarge, color = CmColors.text, modifier = Modifier.fillMaxWidth())
        val big = Modifier.fillMaxWidth().height(52.dp)
        val primary = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary)
        when (ui) {
            AddDeviceUi.Asking -> {
                CircularProgressIndicator(color = CmColors.actionIcon, modifier = Modifier.padding(vertical = 24.dp))
                Text(stringResource(R.string.add_device_asking), style = MaterialTheme.typography.bodyLarge, color = CmColors.text2)
            }
            is AddDeviceUi.Offer -> {
                val tick by produceState(now ?: (System.currentTimeMillis() / 1000), ui.exp, now) {
                    if (now == null) while (value < ui.exp) { kotlinx.coroutines.delay(1_000); value = System.currentTimeMillis() / 1000 }
                }
                val expired = tick >= ui.exp
                Text(
                    stringResource(if (ui.qr != null) R.string.add_device_how else R.string.add_device_how_code),
                    style = MaterialTheme.typography.bodyLarge, color = CmColors.text2, modifier = Modifier.fillMaxWidth(),
                )
                if (ui.qr != null) QrImage(ui.qr, dim = expired, modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f))
                Text(
                    stringResource(R.string.add_device_code, ui.code.chunked(3).joinToString(" ")),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, letterSpacing = 1.sp),
                    color = if (expired) CmColors.text2 else CmColors.text,
                )
                Text(
                    if (expired) stringResource(R.string.add_device_expired) else stringResource(R.string.add_device_left, PairAddText.left(ui.exp, tick)),
                    style = MaterialTheme.typography.bodyMedium, color = if (expired) CmColors.waiting else CmColors.text2,
                )
                if (expired) Button(onRetry, big, colors = primary) { Text(stringResource(R.string.add_device_again)) }
                else Button(onDone, big, colors = primary) { Text(stringResource(R.string.add_device_done)) }
            }
            is AddDeviceUi.Refused -> {
                Text(refusalText(ui), style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.fillMaxWidth())
                Button(onRetry, big, colors = primary) { Text(stringResource(R.string.pair_retry)) }
                OutlinedButton(onDone, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_device_close)) }
            }
            AddDeviceUi.Lost -> {
                Text(stringResource(R.string.add_device_lost), style = MaterialTheme.typography.bodyLarge, color = CmColors.text, modifier = Modifier.fillMaxWidth())
                Button(onRetry, big, colors = primary) { Text(stringResource(R.string.pair_retry)) }
            }
        }
    }
}

@Composable
private fun refusalText(r: AddDeviceUi.Refused): String = when (r.refusal) {
    PairAddText.Refusal.BUSY -> stringResource(R.string.add_device_busy)
    PairAddText.Refusal.NO_KEY -> stringResource(R.string.add_device_no_key)
    PairAddText.Refusal.FULL -> stringResource(R.string.add_device_full)
    PairAddText.Refusal.FAILED -> stringResource(R.string.add_device_failed, r.reason)
    PairAddText.Refusal.OTHER -> stringResource(R.string.add_device_other, r.reason)
}

/**
 * Il QR nero su bianco con il margine di quattro moduli che le fotocamere vogliono; correzione L come il relay (il
 * documento della 1.30 sta in un QR più piccolo). Scaduto, sbiadito: non va più inquadrato.
 */
@Composable
private fun QrImage(text: String, dim: Boolean, modifier: Modifier) {
    val qr = remember(text) { QrCode.encodeText(text, QrCode.Ecc.LOW) }
    val desc = stringResource(R.string.add_device_qr)
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).background(Color.White).semantics { contentDescription = desc }) {
        val n = qr.size + 8
        val cell = size.minDimension / n
        val dark = if (dim) Color(0xFFB0B8C4) else Color.Black
        for (y in 0 until qr.size) for (x in 0 until qr.size) {
            if (qr.getModule(x, y)) drawRect(dark, Offset((x + 4) * cell, (y + 4) * cell), Size(cell + 0.5f, cell + 0.5f))
        }
    }
}
