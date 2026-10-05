package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.pixelbox.cmwatch.ui.tokens.CmColors

/*
 * Lo stile «attuale, più tech, deciso» della casa della master (Franz, 02/10 00:16; valori nella spec
 * 2026-10-01-telefono-casa-master-design.md, sezione «Stile»): griglia di puntini, card come vetro, etichette con riga,
 * led luminoso, numeri da strumento.
 */

/**
 * Griglia di puntini dietro la schermata: 1 dp, bianco al 7,5 %, passo 14 dp. I punti si calcolano una volta per misura e
 * si disegnano in una sola chiamata (Franz, 05/10 12:12: l'app andava in ANR sul Chromebook; migliaia di cerchi disegnati
 * uno per uno a ogni fotogramma tenevano la GPU occupata per secondi).
 */
fun Modifier.dotGrid(): Modifier = drawWithCache {
    val step = 14.dp.toPx()
    val r = 1.dp.toPx() / 2 + 0.25f
    val dot = Color.White.copy(alpha = 0.075f)
    val points = ArrayList<Offset>()
    var y = step / 2
    while (y < size.height) {
        var x = step / 2
        while (x < size.width) { points.add(Offset(x, y)); x += step }
        y += step
    }
    onDrawBehind { drawPoints(points, androidx.compose.ui.graphics.PointMode.Points, dot, strokeWidth = r * 2, cap = androidx.compose.ui.graphics.StrokeCap.Round) }
}

/** Una card come vetro: bordo chiaro al 14 %, angoli 16 dp, riflesso in alto. `tint` colora bordo e velo (Per te: ambra). */
@Composable
fun GlassCard(modifier: Modifier = Modifier, tint: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val border = tint?.copy(alpha = 0.75f) ?: Color.White.copy(alpha = 0.14f)
    Column(
        modifier.fillMaxWidth().clip(shape)
            .background(tint?.copy(alpha = 0.06f) ?: CmColors.surface)
            .background(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.06f), 0.45f to Color.Transparent))
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Etichetta di sezione: 11 sp, maiuscolo, 2 sp fra le lettere, grassetto; con `rule` una riga sottile fino al bordo. */
@Composable
fun RuledLabel(text: String, color: Color, rule: Boolean = true, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text.uppercase(), style = TextStyle(fontSize = 11.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold, color = color))
        if (trailing != null) trailing()
        if (rule) Box(Modifier.weight(1f).height(1.dp).background(color.copy(alpha = 0.4f)))
    }
}

/** Il led: verde con un alone che pulsa in 1,6 s; ambra e fermo con un dato vecchio; fermo nei provini (`LocalStill`). */
@Composable
fun Led(stale: Boolean, dot: Dp = 8.dp) {
    val color = if (stale) CmColors.waiting else CmColors.idle
    val still = LocalStill.current || stale
    // Calcolato a mano come l'asterisco della riga dal vivo: con la scala delle animazioni a zero (il telefono di Franz)
    // le animazioni di Compose saltano alla fine e il led restava fermo (revisione finale 02/10).
    val glow by androidx.compose.runtime.produceState(1f, still) {
        if (!still) while (true) {
            val t = (System.currentTimeMillis() % 1_600) / 1_600.0
            value = 0.35f + 0.65f * (0.5f + 0.5f * kotlin.math.cos(2 * Math.PI * t).toFloat())
            kotlinx.coroutines.delay(50)
        }
    }
    Canvas(Modifier.size(dot + 10.dp)) {
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(color.copy(alpha = 0.35f * glow), radius = dot.toPx() / 2 + 5.dp.toPx() * glow, center = c)
        drawCircle(color.copy(alpha = 0.6f + 0.4f * glow), radius = dot.toPx() / 2, center = c)
    }
}

/** Un numero da strumento: cifre monospaziate grandi, l'unità piccola e grigia. */
@Composable
fun InstrumentNumber(value: String, unit: String, size: Int = 24, color: Color = CmColors.text) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = size.sp, fontWeight = FontWeight.Medium, color = color))
        Text(unit, style = TextStyle(fontSize = 12.sp, color = CmColors.text2), modifier = Modifier.padding(start = 1.dp, bottom = 3.dp))
    }
}

/** Testo piccolo monospaziato per orari e «aggiornato ora · PC». */
val MonoSmall = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CmColors.text2)
