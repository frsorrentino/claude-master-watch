package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.rules.BriefCards
import it.pixelbox.cmwatch.rules.DayBars
import it.pixelbox.cmwatch.rules.QuotaHistory
import it.pixelbox.cmwatch.rules.WorkPanel
import it.pixelbox.cmwatch.wear.ui.theme.BriefNumber
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import kotlinx.coroutines.delay

/*
 * La sezione Lavoro della pagina Quota (Franz, 16/09 13:00, «ok tutte»): una grafica per dato. «Adesso» è una barra a
 * segmenti come le barre impilate di Apple Fitness, «Contesto» barrette per sessione, «Oggi» colonne per ora come i
 * passi, il ritmo della finestra una linea con la proiezione tratteggiata. Gli anelli restano alla quota, che misura un
 * traguardo. Stessa card del brief: etichetta verde, numero grande, stessi margini.
 */

/** Guscio comune: la card del brief con l'etichetta verde in testa. */
@Composable
private fun WorkShell(
    label: String,
    transformation: SurfaceTransformation?,
    modifier: Modifier,
    labelColor: Color = CmColors.briefLabel,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = CmColors.briefCard, contentColor = CmColors.briefBig),
        contentPadding = PaddingValues(start = 15.dp, top = 13.dp, end = 15.dp, bottom = 13.dp),
        transformation = transformation,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        content()
    }
}

/** Numero grande con l'unità piccola accanto, come nelle card del brief. */
@Composable
private fun BigNumber(value: String, unit: String?, animate: Boolean, color: Color = CmColors.briefBig) {
    Row(verticalAlignment = Alignment.Bottom) {
        RollingText(value, style = BriefNumber, color = color, animate = animate)
        unit?.let {
            Spacer(Modifier.width(4.dp))
            Text(
                it, style = MaterialTheme.typography.bodyMedium, color = CmColors.briefSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

/** Un valore da 0 a 1 che si riempie quando la card entra nell'inquadratura, con la molla lenta del motion scheme. */
@Composable
private fun fillOnEntry(target: Float, animate: Boolean, visible: Boolean, delayMs: Long = 0): Animatable<Float, *> {
    val a = remember { Animatable(if (animate) 0f else target) }
    val molla = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
    LaunchedEffect(target, animate, visible) {
        when {
            !animate -> a.snapTo(target)
            visible -> { if (delayMs > 0) delay(delayMs); a.animateTo(target, molla) }
            else -> a.snapTo(0f)
        }
    }
    return a
}

private fun segColour(s: WorkPanel.Seg): Color = when (s) {
    WorkPanel.Seg.WAITING -> CmColors.briefWarn
    WorkPanel.Seg.WORKING -> CmColors.briefRing
    WorkPanel.Seg.IDLE -> CmColors.briefGood
}

private fun toneColour(t: BriefCards.Tone): Color = when (t) {
    BriefCards.Tone.ALERT -> CmColors.briefAlertRing
    BriefCards.Tone.WARN -> CmColors.briefWarn
    else -> CmColors.briefRing
}

/**
 * «Adesso»: quante sessioni lavorano, e sotto una barra con un segmento per sessione viva nei colori dei badge (ambra
 * aspetta te, azzurro lavora, verde ferma). I segmenti crescono da sinistra uno dopo l'altro entrando nell'inquadratura.
 */
@Composable
fun NowCard(
    now: WorkPanel.Now,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visible: Boolean = true,
    onClick: () -> Unit = {},
) {
    WorkShell(stringResource(R.string.work_now), transformation, modifier, onClick = onClick) {
        BigNumber(now.working.toString(), pluralStringResource(R.plurals.work_now_working, now.working), animate)
        Spacer(Modifier.height(8.dp))
        if (now.segments.isEmpty()) {
            Text(stringResource(R.string.work_none_live), style = MaterialTheme.typography.bodySmall, color = CmColors.briefSecondary)
        } else {
            Row(Modifier.fillMaxWidth().height(10.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                now.segments.forEachIndexed { i, seg ->
                    val crescita = fillOnEntry(1f, animate, visible, delayMs = 70L * i)
                    Box(
                        Modifier.weight(1f).height(10.dp)
                            .graphicsLayer { scaleX = crescita.value; transformOrigin = TransformOrigin(0f, 0.5f) }
                            .background(segColour(seg), RoundedCornerShape(percent = 50))
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val legenda = listOfNotNull(
                now.waiting.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.work_legend_waiting, it, it) },
                now.working.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.work_legend_working, it, it) },
                now.idle.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.work_legend_idle, it, it) },
            ).joinToString(" · ")
            Text(legenda, style = MaterialTheme.typography.bodySmall, color = CmColors.briefSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** «Contesto»: una riga per sessione, nome e percentuale, e sotto la sua barretta nel colore delle soglie. */
@Composable
fun ContextListCard(
    rows: List<WorkPanel.Ctx>,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visible: Boolean = true,
) {
    if (rows.isEmpty()) return
    WorkShell(stringResource(R.string.work_context), transformation, modifier) {
        Spacer(Modifier.height(4.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            rows.forEachIndexed { i, r ->
                val ink = toneColour(r.tone)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        r.name, style = MaterialTheme.typography.bodySmall, color = CmColors.briefBig,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.card_context_pct, r.pct), style = MaterialTheme.typography.bodySmall, color = ink, maxLines = 1)
                }
                val pieno = fillOnEntry((r.pct / 100f).coerceIn(0f, 1f), animate, visible, delayMs = 80L * i)
                Canvas(Modifier.fillMaxWidth().height(5.dp)) {
                    val raggio = CornerRadius(size.height / 2, size.height / 2)
                    drawRoundRect(CmColors.briefTrack, size = size, cornerRadius = raggio)
                    val w = size.width * pieno.value
                    if (w > 0f) drawRoundRect(ink, size = Size(maxOf(w, size.height), size.height), cornerRadius = raggio)
                }
            }
        }
    }
}

/**
 * «Oggi»: una colonna per ora fino all'ora corrente, alta quanto il lavoro di quell'ora, come le barre dei passi. Sotto,
 * le ore 0 6 12 18 come riferimento. Il numero grande è il totale degli eventi della giornata.
 */
@Composable
fun TodayCard(
    bars: List<DayBars.Bar>,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visible: Boolean = true,
) {
    val totale = bars.sumOf { it.count }
    val picco = DayBars.peak(bars).coerceAtLeast(1)
    WorkShell(stringResource(R.string.work_today), transformation, modifier) {
        BigNumber(totale.toString(), pluralStringResource(R.plurals.work_today_events, totale), animate)
        Spacer(Modifier.height(8.dp))
        val crescita = fillOnEntry(1f, animate, visible)
        Canvas(Modifier.fillMaxWidth().height(38.dp)) {
            val passo = size.width / 24f
            val larghezza = passo * 0.62f
            for (h in 0 until 24) {
                val x = h * passo + (passo - larghezza) / 2
                val count = bars.firstOrNull { it.hour == h }?.count
                if (count == null) {
                    // Ore non ancora arrivate: un puntino sulla base, non una colonna vuota che direbbe «niente».
                    drawCircle(CmColors.briefTrack, radius = 1.5.dp.toPx(), center = Offset(x + larghezza / 2, size.height - 1.5.dp.toPx()))
                    continue
                }
                val alto = if (count == 0) 2.dp.toPx() else (size.height * count / picco) * crescita.value
                drawRoundRect(
                    if (count == 0) CmColors.briefTrack else CmColors.briefRing,
                    topLeft = Offset(x, size.height - alto), size = Size(larghezza, alto),
                    cornerRadius = CornerRadius(larghezza / 2, larghezza / 2),
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Row(Modifier.fillMaxWidth()) {
            listOf("0", "6", "12", "18").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = CmColors.briefSecondary, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** Le domande aperte, in ambra: quante, e la più vecchia con da quanto aspetta. Un tocco la apre. */
@Composable
fun QuestionsWorkCard(
    q: WorkPanel.Questions,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    onClick: () -> Unit = {},
) {
    WorkShell(stringResource(R.string.brief_questions), transformation, modifier, labelColor = CmColors.briefWarn, onClick = onClick) {
        BigNumber(q.count.toString(), null, animate, color = CmColors.briefWarn)
        Text(
            stringResource(R.string.work_question_oldest, q.oldest, q.age), style = MaterialTheme.typography.bodySmall,
            color = CmColors.briefSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

/** La coda della notte: quante in coda, quale gira, e una barra di quanta coda è già passata. */
@Composable
fun NightWorkCard(
    n: WorkPanel.NightQueue,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visible: Boolean = true,
) {
    WorkShell(stringResource(R.string.brief_night), transformation, modifier) {
        BigNumber(n.queued.toString(), null, animate)
        Text(
            n.running?.let { stringResource(R.string.brief_running, it) } ?: stringResource(R.string.brief_nothing_running),
            style = MaterialTheme.typography.bodySmall, color = CmColors.briefSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        val pieno = fillOnEntry(n.progress, animate, visible)
        Canvas(Modifier.fillMaxWidth().height(6.dp)) {
            val raggio = CornerRadius(size.height / 2, size.height / 2)
            drawRoundRect(CmColors.briefTrack, size = size, cornerRadius = raggio)
            val w = size.width * pieno.value
            if (w > 0f) drawRoundRect(CmColors.briefRing, size = Size(maxOf(w, size.height), size.height), cornerRadius = raggio)
        }
    }
}

/**
 * «Ritmo 5 ore»: un grafico della finestra (Franz, 16/09 14:18: «non capisco, dovrebbe essere un grafico?»). In
 * orizzontale la finestra dall'inizio al reset, con gli orari ai due capi; in verticale la quota da 0 a 100. Linea piena
 * = consumo registrato, tratteggio = dove si arriva al reset a questo ritmo, tacca verticale = adesso. Con pochi dati il
 * grafico non si disegna: un trattino di mezz'ora senza orari non diceva niente.
 */
@Composable
fun PaceCard(
    pace: QuotaHistory.Pace,
    current: Int,
    resetAt: Long,
    now: Long,
    startLabel: String,
    resetLabel: String,
    transformation: SurfaceTransformation?,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    visible: Boolean = true,
) {
    val proiettato = pace.projected
    val ink = when {
        (proiettato ?: current) >= 100 -> CmColors.briefAlertRing
        (proiettato ?: current) >= 90 -> CmColors.briefWarn
        else -> CmColors.briefRing
    }
    val punti = pace.points
    // Almeno due campioni a dieci minuti l'uno dall'altro: sotto, la linea è un puntino e la proiezione un'invenzione.
    val grafico = punti.size >= 2 && punti.last().ts - punti.first().ts >= 600
    WorkShell(stringResource(R.string.work_pace), transformation, modifier) {
        if (proiettato != null && grafico) {
            BigNumber(proiettato.toString(), stringResource(R.string.work_pace_at, resetLabel), animate, color = ink)
            Text(stringResource(R.string.work_pace_projection), style = MaterialTheme.typography.bodySmall, color = CmColors.briefSecondary, maxLines = 1)
        } else {
            BigNumber(current.toString(), stringResource(R.string.work_pace_now), animate)
            Text(
                stringResource(R.string.work_pace_unknown), style = MaterialTheme.typography.bodySmall,
                color = CmColors.briefSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (!grafico) return@WorkShell
        Spacer(Modifier.height(8.dp))
        val disegno = fillOnEntry(1f, animate, visible)
        val inizio = resetAt - QuotaHistory.WINDOW_S
        Canvas(Modifier.fillMaxWidth().height(40.dp)) {
            fun x(ts: Long) = size.width * ((ts - inizio).toFloat() / QuotaHistory.WINDOW_S).coerceIn(0f, 1f)
            fun y(pct: Int) = size.height * (1f - (pct / 100f).coerceIn(0f, 1f))
            // Fondo e tetto (0 e 100 %): due linee sottili.
            drawLine(CmColors.briefTrack, Offset(0f, 0.5.dp.toPx()), Offset(size.width, 0.5.dp.toPx()), strokeWidth = 1.dp.toPx())
            drawLine(CmColors.briefTrack, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            // Adesso: una tacca verticale tratteggiata, così si vede quanta finestra resta.
            drawLine(
                CmColors.briefSecondary.copy(alpha = 0.5f), Offset(x(now), 0f), Offset(x(now), size.height),
                strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 3.dp.toPx())),
            )
            val tratto = 2.5.dp.toPx()
            val quanti = (punti.size * disegno.value).toInt().coerceAtMost(punti.size)
            for (i in 1 until quanti) {
                drawLine(ink, Offset(x(punti[i - 1].ts), y(punti[i - 1].pct)), Offset(x(punti[i].ts), y(punti[i].pct)), strokeWidth = tratto, cap = StrokeCap.Round)
            }
            val ultimo = punti.last()
            if (disegno.value >= 1f) {
                if (proiettato != null) {
                    drawLine(
                        ink.copy(alpha = 0.7f), Offset(x(ultimo.ts), y(ultimo.pct)), Offset(size.width, y(proiettato)),
                        strokeWidth = tratto, cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())),
                    )
                }
                drawCircle(ink, radius = 3.5.dp.toPx(), center = Offset(x(ultimo.ts), y(ultimo.pct)))
            }
        }
        Spacer(Modifier.height(3.dp))
        // Gli orari ai due capi: inizio della finestra a sinistra, reset a destra.
        Row(Modifier.fillMaxWidth()) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = CmColors.briefSecondary, modifier = Modifier.weight(1f))
            Text(resetLabel, style = MaterialTheme.typography.labelSmall, color = CmColors.briefSecondary)
        }
    }
}

/** La riga in fondo alla pagina al posto della card «Aggiornato»: grigia, rossa solo con il PC fermo. */
@Composable
fun UpdatedFooter(u: WorkPanel.Updated, modifier: Modifier = Modifier) {
    val testo = when {
        u.stale -> stringResource(R.string.work_stale, u.minutes, u.host)
        u.minutes <= 0 -> stringResource(R.string.work_updated_now, u.host)
        else -> stringResource(R.string.work_updated_ago, u.minutes, u.host)
    }
    Text(
        testo, style = MaterialTheme.typography.bodySmall, color = if (u.stale) CmColors.briefAlertRing else CmColors.briefSecondary,
        textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier.fillMaxWidth(),
    )
}
