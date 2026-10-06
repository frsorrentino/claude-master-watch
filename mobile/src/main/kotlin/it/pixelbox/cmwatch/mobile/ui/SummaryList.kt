package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Session
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterHome
import it.pixelbox.cmwatch.rules.OutsideSessions
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.SessionsText
import it.pixelbox.cmwatch.rules.Summary
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il riepilogo unico (design 03/10, tavola 4): ogni sessione una volta, nei gruppi del bisogno, una riga aperta alla
 * volta; aperta mostra anche i dettagli delle vecchie card di Sessioni. Sotto, «Chiuse · N» e le righe di servizio.
 */
@Composable
fun SummaryList(
    model: Summary.Model, onOpen: (String) -> Unit, onAnswer: (String, Int) -> Unit, onStep: (String, String) -> Unit,
    onService: (MasterHome.Row) -> Unit, onClosed: () -> Unit, initiallyOpen: String? = null,
    /** Il tablet (Franz, 04/10 14:31): nell'altezza in più, sotto, le schede di «Utilizzo e limiti». */
    footer: (@Composable () -> Unit)? = null,
    /** Contratto 1.37: i compiti che aspettano l'ok, in cima; l'ok con la nota (vuota = «ok»). */
    approvals: List<it.pixelbox.cmwatch.contract.Approval> = emptyList(), onApprove: (task: String, note: String) -> Unit = { _, _ -> },
    /** Contratto 1.37: «Chiudi» per le sessioni finite o doppie senza finestra; `canExit` = il PC accetta /exit. */
    canExit: Boolean = false, onClose: (String) -> Unit = {},
    now: Long = System.currentTimeMillis() / 1000,
) {
    var expanded by rememberSaveable { mutableStateOf(initiallyOpen) }
    // Una lista «pigra» con le card riconosciute dal nome della sessione: quando una sessione cambia gruppo (da «Al lavoro» a
    // «Ha finito») la sua card scivola al posto nuovo e le altre si spostano (osservazioni del 03/10, transizione 4).
    val off = animationsOff()
    val moving: @Composable androidx.compose.foundation.lazy.LazyItemScope.() -> Modifier = {
        if (off) Modifier.animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
        else Modifier.animateItem(
            fadeInSpec = androidx.compose.animation.core.tween(150), fadeOutSpec = androidx.compose.animation.core.tween(150),
            placementSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                visibilityThreshold = androidx.compose.ui.unit.IntOffset.VisibilityThreshold,
            ),
        )
    }
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Contratto 1.37: «Da approvare» prima di «Ti aspetta» (mockup approvato il 05/10 21:07).
        if (approvals.isNotEmpty()) {
            item(key = "h-approvals") { Box(moving()) { GroupHeader(stringResource(R.string.approvals_group) + " · " + approvals.size, CmColors.advice) } }
            items(approvals, key = { "a-" + it.task }) { a -> Box(moving()) { ApprovalCard(a, now, onApprove) } }
        }
        model.rows.groupBy { it.group }.forEach { (group, rows) ->
            item(key = "h-${group.name}") { Box(moving()) { GroupHeader(pluralStringResource(groupLabel(group), rows.size, rows.size), groupTone(group)) } }
            items(rows, key = { "s-" + it.session.name }) { r ->
                val s = r.session
                val id = group.name + ":" + s.name
                Box(moving()) {
                    SummaryCard(
                        r, expanded == id, onToggle = { expanded = if (expanded == id) null else id },
                        onAnswer = { n -> onAnswer(s.name, n) }, onStep = { t -> onStep(s.name, t) }, onOpen = { onOpen(s.name) },
                        firstFilled = approvals.isEmpty(), canExit = canExit, onClose = { onClose(s.name) }, now = now,
                    )
                }
            }
        }
        // «Fuori dalle sessioni» (mockup A, Franz 03/10 22:34): un titolo vero che separa le sessioni dal resto, poi le
        // categorie con icona e conteggio, dalla più urgente; ognuna nella sua card.
        val outside = OutsideSessions.groups(model.service, model.closed)
        if (outside.isNotEmpty()) {
            item(key = "out-head") { Box(moving()) { OutsideHeader() } }
            outside.forEach { g ->
                item(key = "out-${g.category}") { Box(moving()) { CategoryHeader(g) } }
                item(key = "out-${g.category}-card") {
                    Box(moving()) { if (g.category == OutsideSessions.Category.CLOSED) ClosedCard(g.closed, onClosed) else OutsideCard(g.rows, onService) }
                }
            }
        }
        footer?.let { f -> item(key = "footer") { f() } }
    }
}

internal fun groupLabel(g: Summary.Group) = when (g) {
    Summary.Group.WAITING -> R.plurals.summary_waiting
    Summary.Group.FINISHED -> R.plurals.summary_finished
    Summary.Group.WORKING -> R.plurals.summary_working
    Summary.Group.STILL -> R.plurals.summary_still
}

internal fun groupTone(g: Summary.Group): Color = when (g) {
    Summary.Group.WAITING -> CmColors.briefWarn
    Summary.Group.FINISHED -> CmColors.briefGood
    Summary.Group.WORKING -> CmColors.briefRing
    Summary.Group.STILL -> CmColors.text2
}

/** Priorità e «modello · effort · account» (il contesto sta in testa alla card, l'obiettivo sopra il testo). */
@Composable
private fun details(s: Session): List<String> = listOfNotNull(
    SessionsText.priority(s, stringResource(R.string.low_priority), stringResource(R.string.low_priority_offered)),
    listOfNotNull(ModelText.short(s.model), s.effort, s.account).joinToString(" · ").ifEmpty { null },
)

/**
 * Una sessione del riepilogo (Franz, 03/10 15:18, variante B): una card con il badge (colore della sessione, cerchio o
 * quadrato dell'account, glifo dello stato), nome, contesto e quota delle 5 ore; sotto l'ultimo esito o la domanda su due righe e la
 * barretta del contesto. Aperta: il testo intero, i dettagli, le opzioni o i consigli e la conversazione.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SummaryCard(
    r: Summary.Row, open: Boolean, onToggle: () -> Unit,
    onAnswer: (Int) -> Unit, onStep: (String) -> Unit, onOpen: () -> Unit,
    firstFilled: Boolean = true, canExit: Boolean = false, onClose: () -> Unit = {}, now: Long = System.currentTimeMillis() / 1000,
) {
    val s = r.session
    var closeAsk by rememberSaveable(s.name) { mutableStateOf(false) }
    // Il tasto di un passo toccato resta segnato «mandato» finché la sessione non cambia esito (Franz, 06/10 12:32).
    var sentSteps by rememberSaveable(s.name, s.outcome?.at) { mutableStateOf(listOf<String>()) }
    val waiting = r.group == Summary.Group.WAITING
    val parsed = androidx.compose.runtime.remember(r.text) { it.pixelbox.cmwatch.rules.NextSteps.parse(r.text.orEmpty()) }
    // L'esito per primo e senza l'etichetta «Esito:»; la domanda così com'è.
    val shown = if (waiting) parsed.text else s.outcome?.let { o -> it.pixelbox.cmwatch.rules.OutcomeText.summary(o) } ?: parsed.text
    val body = it.pixelbox.cmwatch.rules.Markdown.parse(shown).text.trim()
    val fill = if (waiting) androidx.compose.ui.graphics.lerp(CmColors.surfaceLow, CmColors.briefWarn, 0.06f) else CmColors.surfaceLow
    Column(
        // La chat si restringe verso la sua card durante il gesto indietro.
        Modifier.fly("card-${s.id}").fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(fill).smoothSize()
            // Franz, 03/10 17:05: il tocco sulla card porta dritto alla sessione; il tasto ▼ la apre sul posto.
            .handCursor().clickable(onClick = onOpen).handCursor().padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SessionBadge(s, 24.dp)
            Text(
                s.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                color = CmColors.text, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip, modifier = Modifier.weight(1f),
            )
            if (s.duplicateOf != null) Tag(stringResource(R.string.cleanup_dup_tag), CmColors.briefWarn)
            // «ctx 17%»: il contesto occupato, non un avanzamento del lavoro (osservazioni del 03/10).
            s.context?.let { Text(stringResource(R.string.ctx_short, it), style = MonoSmall) }
            // La quota delle 5 ore dell'account al posto dell'età della sessione (Franz, 03/10 20:31: «non è un'informazione
            // rilevante»); il dato vecchio nel colore dell'attesa, come nel Quadro.
            r.quota?.let { q -> q.h5?.let { Text(stringResource(R.string.quota_h5_short, it), style = MonoSmall, color = if (q.stale) CmColors.waiting else CmColors.text2, maxLines = 1) } }
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                androidx.compose.material3.Icon(
                    if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    stringResource(if (open) R.string.steps_close else R.string.steps_open), tint = CmColors.text2,
                )
            }
        }
        // L'obiettivo della sessione, stabile, sopra l'attività del momento (osservazioni del 03/10): si legge anche dopo ore.
        SessionsText.goalLine(s, stringResource(R.string.goal))?.let { g ->
            Text(g, style = MaterialTheme.typography.bodySmall, color = CmColors.briefLabel, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip)
        }
        if (body.isNotBlank()) Text(
            body, style = MaterialTheme.typography.bodyMedium, color = if (waiting || open) CmColors.text else CmColors.text2,
            maxLines = if (open) Int.MAX_VALUE else 2, overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
        )
        if (open) {
            details(s).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Clip) }
        }
        // Le opzioni della domanda subito, anche a card chiusa: rispondere è il motivo per cui la card è in cima.
        if (waiting) s.question?.let { q ->
            var holdHint by rememberSaveable(q.id) { mutableStateOf(false) }
            // Sul fondo della card il tonale di sempre quasi spariva (provini 03/10): un velo del colore primario.
            QuestionOptions(q, firstFilled = firstFilled, holdHint = holdHint, onHold = { holdHint = true }, onAnswer = onAnswer, tonal = CmColors.primary.copy(alpha = 0.12f))
        }
        // I consigli come tasti, sempre visibili (Franz, 03/10 17:27): il tocco li manda subito a quella sessione, che li
        // prende a fine turno se sta lavorando.
        if (!waiting && parsed.steps.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Contratto 1.38, variante 2: chi sblocca ha il filo ambra e il lucchetto (dal «!» o da `next_steps`).
            val blocking = parsed.blocking + s.nextSteps.orEmpty().filter { it.blocking }.map { it.text }
            parsed.steps.forEach { step -> StepChip(step, step in blocking, sent = step in sentSteps) { sentSteps = sentSteps + step; onStep(step) } }
        }
        // Contratto 1.37: compito chiuso o doppione, a sessione ferma; «Chiudi» solo senza finestra. Chi ha finito resta qui e
        // dice di essere aperta; la conferma sta nella card, senza finestre (Franz, 06/10 10:57-12:45).
        it.pixelbox.cmwatch.rules.MasterService.cleanup(s, canExit, now)?.let { c ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val detail = when {
                    s.attached -> null
                    c.kind == it.pixelbox.cmwatch.rules.MasterService.Cleanup.Kind.DUPLICATE -> stringResource(R.string.cleanup_duplicate, c.of.orEmpty())
                    else -> s.outcome?.at?.let { stringResource(R.string.cleanup_finished_at, hm(it)) } ?: stringResource(R.string.cleanup_finished)
                }
                val open = stringResource(R.string.cleanup_open)
                Text(
                    if (detail == null) androidx.compose.ui.text.AnnotatedString(stringResource(R.string.cleanup_attached))
                    else androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(color = CmColors.idle, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)) { append("● $open") }
                        append(" · $detail")
                    },
                    style = MaterialTheme.typography.bodySmall, color = CmColors.text2, modifier = Modifier.weight(1f),
                )
                if (c.canClose && !closeAsk) Surface(onClick = { closeAsk = true }, shape = CircleShape, color = CmColors.surfaceHigh, contentColor = CmColors.text) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.PowerSettingsNew, null, tint = CmColors.briefAlertRing, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.cleanup_close, s.name), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            if (c.canClose && closeAsk) androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(stringResource(R.string.close_inline), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, modifier = Modifier.padding(end = 4.dp))
                androidx.compose.material3.TextButton(onClick = { closeAsk = false }) { Text(stringResource(R.string.close_keep), color = CmColors.actionIcon) }
                androidx.compose.material3.Button(
                    onClick = { closeAsk = false; onClose() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CmColors.gone, contentColor = Color.White),
                ) { Text(stringResource(R.string.cleanup_close, s.name)) }
            }
        }
        s.context?.let { pct ->
            val bar = when (it.pixelbox.cmwatch.rules.SessionMeters.contextTone(pct)) {
                it.pixelbox.cmwatch.rules.BriefCards.Tone.ALERT -> CmColors.briefAlertRing
                it.pixelbox.cmwatch.rules.BriefCards.Tone.WARN -> CmColors.briefWarn
                else -> CmColors.briefRing
            }
            Box(Modifier.padding(end = 4.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)).background(CmColors.briefTrack)) {
                Box(Modifier.fillMaxWidth(it.pixelbox.cmwatch.rules.SessionMeters.contextFraction(pct) ?: 0f).fillMaxHeight().background(bar))
            }
        }
    }
}

/** «Chiuse · N»: una riga sola, anche con molte sessioni chiuse; apre l'elenco con «Riapri». */

/** Il titolo della parte sotto le sessioni: più forte delle etichette dei gruppi, con il filo e una riga che spiega. */
@Composable
private fun OutsideHeader() {
    Column(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 18.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.outside_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = CmColors.text)
            Box(Modifier.weight(1f).height(1.dp).background(CmColors.line))
        }
        Text(stringResource(R.string.outside_sub), style = MaterialTheme.typography.bodySmall, color = CmColors.stale)
    }
}

/**
 * L'etichetta di una categoria: icona, nome e conteggio in maiuscoletto, neutri (le etichette colorate sono gli stati delle
 * sessioni, sopra); solo «Da sistemare» nel colore dell'attesa.
 */
@Composable
private fun CategoryHeader(g: OutsideSessions.Group) {
    val (icon, label) = when (g.category) {
        OutsideSessions.Category.FIX -> Icons.Rounded.WarningAmber to R.string.cat_fix
        OutsideSessions.Category.SCHEDULED -> Icons.Rounded.Schedule to R.string.cat_scheduled
        OutsideSessions.Category.RESUME -> Icons.Rounded.Replay to R.string.cat_resume
        OutsideSessions.Category.NIGHT -> Icons.Rounded.Bedtime to R.string.cat_night
        OutsideSessions.Category.CLOSED -> Icons.Rounded.Inventory2 to R.string.cat_closed
    }
    val tone = if (g.category == OutsideSessions.Category.FIX) CmColors.waiting else CmColors.text2
    val name = stringResource(label)
    val text = if (g.category == OutsideSessions.Category.NIGHT) name else stringResource(R.string.cat_count, name, g.count)
    Row(Modifier.padding(start = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tone, modifier = Modifier.size(16.dp))
        Text(text.uppercase(), style = MonoSmall.copy(color = tone))
        if (g.category == OutsideSessions.Category.RESUME) Text(stringResource(R.string.cat_resume_note), style = MaterialTheme.typography.labelSmall, color = CmColors.stale)
    }
}

/** Le righe di una categoria in una card, divise da un filo; ognuna con il suo tasto a destra. */
@Composable
private fun OutsideCard(rows: List<MasterHome.Row>, onAction: (MasterHome.Row) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow)) {
        rows.forEachIndexed { i, row ->
            if (i > 0) Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(1.dp).background(CmColors.line))
            OutsideRow(row, onAction)
        }
    }
}

@Composable
private fun OutsideRow(row: MasterHome.Row, onAction: (MasterHome.Row) -> Unit) {
    val n = row.number ?: 0
    val (title, detail, action) = when (row.kind) {
        MasterHome.Kind.CONTEXT -> Triple(stringResource(R.string.fy_context, row.title, n), stringResource(R.string.fy_context_detail), R.string.fy_btn_handoff)
        MasterHome.Kind.SCHEDULED -> Triple(
            androidx.compose.ui.res.pluralStringResource(R.plurals.fy_scheduled, n.coerceAtLeast(1), n.coerceAtLeast(1)),
            row.at?.let { stringResource(R.string.fy_scheduled_detail, OUT_HM.format(java.time.Instant.ofEpochSecond(it).atZone(java.time.ZoneId.systemDefault()))) },
            R.string.fy_btn_see,
        )
        // Il nome del progetto basta: «prossimo passo» lo dice già l'etichetta della categoria.
        MasterHome.Kind.NEXT_STEP -> Triple(row.title, row.detail, R.string.fy_btn_start)
        MasterHome.Kind.NIGHT_REPORT -> Triple(row.title, row.detail?.lineSequence()?.firstOrNull { it.isNotBlank() }, R.string.fy_btn_listen)
        MasterHome.Kind.NIGHT -> Triple(
            if (n > 0) stringResource(R.string.fy_night_queued, n) else stringResource(R.string.fy_night_empty_short),
            stringResource(R.string.fy_night_detail), R.string.fy_btn_add,
        )
        MasterHome.Kind.QUESTION, MasterHome.Kind.FINISHED -> Triple(row.title, row.detail, R.string.fy_btn_see)
    }
    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (row.kind == MasterHome.Kind.NEXT_STEP) Box(Modifier.size(34.dp).clip(CircleShape).background(CmColors.surface), contentAlignment = Alignment.Center) {
            Text(row.title.take(1).uppercase(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = CmColors.actionIcon)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = CmColors.text)
            detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Clip) }
        }
        Surface(onClick = { onAction(row) }, shape = CircleShape, color = CmColors.surface, contentColor = CmColors.primary) {
            Text(stringResource(action), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp))
        }
    }
}

private val OUT_HM = java.time.format.DateTimeFormatter.ofPattern("HH:mm")

/** Le sessioni chiuse in una riga: i primi tre nomi e quante altre; il tocco apre l'elenco. */
@Composable
private fun ClosedCard(closed: List<Session>, onClick: () -> Unit) {
    val names = closed.take(3).joinToString(", ") { it.name } + if (closed.size > 3) " " + stringResource(R.string.closed_more, closed.size - 3) else ""
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).handCursor().clickable(onClick = onClick).handCursor().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(names, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, pluralStringResource(R.plurals.summary_closed, closed.size, closed.size), tint = CmColors.text2)
    }
}

/** Un Prossimo come tasto; con `blocking` (contratto 1.38, variante 2) il filo ambra e il lucchetto aperto. */
@Composable
internal fun StepChip(text: String, blocking: Boolean, sent: Boolean = false, onClick: () -> Unit) {
    val label = stringResource(R.string.step_sent, text)
    Surface(
        onClick = onClick, enabled = !sent, shape = CircleShape,
        color = if (sent) CmColors.idle.copy(alpha = 0.14f) else CmColors.primary.copy(alpha = 0.12f), contentColor = if (sent) CmColors.text2 else CmColors.text,
        border = if (blocking && !sent) androidx.compose.foundation.BorderStroke(1.5.dp, CmColors.waiting.copy(alpha = 0.65f)) else null,
        modifier = if (sent) Modifier.semantics { contentDescription = label } else Modifier,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (sent) Icon(Icons.Rounded.Check, null, tint = CmColors.idle, modifier = Modifier.size(16.dp))
            else if (blocking) Icon(Icons.Rounded.LockOpen, null, tint = CmColors.waiting, modifier = Modifier.size(16.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Un'etichetta piccola nel colore dato, «DOPPIONE» o «PRODUZIONE». */
@Composable
private fun Tag(text: String, tone: Color) = Text(
    text.uppercase(), style = MonoSmall.copy(color = tone, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
    modifier = Modifier.clip(CircleShape).background(tone.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 3.dp),
)

/** Contratto 1.37: un compito che aspetta l'ok — cosa esce, dove, da quanto — con «Approva» e la conferma. */
@Composable
private fun ApprovalCard(a: it.pixelbox.cmwatch.contract.Approval, now: Long, onApprove: (String, String) -> Unit) {
    var ask by rememberSaveable(a.task) { mutableStateOf(false) }
    var note by rememberSaveable(a.task) { mutableStateOf("") }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CmColors.surfaceLow).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Rounded.Verified, null, tint = CmColors.advice, modifier = Modifier.size(24.dp))
            Text(a.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = CmColors.text, modifier = Modifier.weight(1f))
            if (a.deploy) Tag(stringResource(R.string.approval_prod), CmColors.prod)
        }
        listOfNotNull(
            a.what?.let { stringResource(R.string.approval_what) to it },
            a.where?.let { stringResource(R.string.approval_where) to it },
            stringResource(R.string.approval_asked) to it.pixelbox.cmwatch.contract.Durations.since(a.requestedAt, now),
        ).forEach { (k, v) ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(k, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.width(64.dp))
                Text(v, style = MaterialTheme.typography.bodyMedium, color = CmColors.text)
            }
        }
        androidx.compose.material3.Button(
            onClick = { ask = true }, modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
        ) { Text(stringResource(R.string.approve)) }
    }
    if (ask) androidx.compose.material3.AlertDialog(
        onDismissRequest = { ask = false }, containerColor = CmColors.surface,
        title = { Text(if (a.deploy) stringResource(R.string.approve_prod_title) else stringResource(R.string.approve_title, a.title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.approve_text, a.title, a.what.orEmpty(), a.where.orEmpty()))
                androidx.compose.material3.OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.approve_note)) }, placeholder = { Text("ok") }, singleLine = true)
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { ask = false; onApprove(a.task, note) }) { Text(stringResource(R.string.approve), color = CmColors.actionIcon) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = { ask = false }) { Text(stringResource(R.string.cancel), color = CmColors.text2) } },
    )
}
