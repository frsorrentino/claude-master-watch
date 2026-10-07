@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Choices
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.MasterService
import it.pixelbox.cmwatch.rules.ModelText
import it.pixelbox.cmwatch.rules.Tune
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il pannello Modello ed effort (bozza approvata da Franz il 07/10 alle 00:06, docs/mockup/2026-10-06-modello-effort/):
 * scende dalla pillola come il menu dell'app e «Vai a»; in testa il consiglio di fable-director sul compito (contratto
 * 1.37 A), poi i modelli con il cerchio, il nome e la riga che li spiega, e l'effort come gruppo di tasti connessi. Ogni
 * scelta chiude il pannello; «Usa il consiglio» cambia solo quello che differisce. Le stesse regole in Header.svelte.
 */
@Composable
fun TunePanel(
    choices: Choices, model: String?, effort: String?, advice: MasterService.AdviceView?,
    onModel: (String) -> Unit, onEffort: (String) -> Unit, onDismiss: () -> Unit,
) = PanelShell(
    onDismiss,
    header = { Text(stringResource(R.string.tune_head), style = MaterialTheme.typography.bodyMedium, color = CmColors.text2, modifier = Modifier.weight(1f)) },
    origin = TransformOrigin(0f, 0f),
) { close ->
    advice?.let { a ->
        AdviceCard(a, choices, model, effort) { go -> close(); go.model?.let { onModel(it.id) }; go.effort?.let(onEffort) }
    }
    SectionLabel(stringResource(R.string.model_title))
    choices.models.forEach { m ->
        val label = m.label ?: ModelText.short(m) ?: m.id
        val on = Tune.sameModel(m.id, model)
        ModelRow(label, modelSub(m.id), on, recommended = !on && advice != null && Tune.sameModel(m.id, advice.model)) { close(); onModel(m.id) }
    }
    SectionLabel(stringResource(R.string.effort_title))
    EffortGroup(choices.efforts, effort, advice?.effort) { close(); onEffort(it) }
    effort?.let { e ->
        Text(
            listOfNotNull(e, effortSub(e)).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = CmColors.text2,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 6.dp),
        )
    }
}

/**
 * Il consiglio sul compito: fondo lilla tenue, il motivo sotto; il costo e «Usa il consiglio» solo se è diverso. Uguale
 * alla scelta, verde con ✓: è una conferma, non un suggerimento (Franz, 07/10 21:21). Senza motivo, nessuna riga vuota.
 */
@Composable
private fun AdviceCard(a: MasterService.AdviceView, choices: Choices, model: String?, effort: String?, onUse: (MasterService.AdviceSteps) -> Unit) {
    val go = MasterService.adviceSteps(a, choices, model, effort)
    val same = go.model == null && go.effort == null
    val m = choices.models.firstOrNull { Tune.sameModel(it.id, a.model) }
    val label = "${m?.label ?: ModelText.short(m) ?: a.model} · ${a.effort}"
    val tint = if (same) CmColors.briefGood else CmColors.advice
    Column(
        Modifier.padding(horizontal = 12.dp, vertical = 2.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(tint.copy(alpha = .10f)).padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                Icon(if (same) Icons.Rounded.Check else Icons.Rounded.AutoAwesome, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.advice_head).uppercase(), style = MonoSmall.copy(color = tint, fontWeight = FontWeight.SemiBold))
                Text(if (same) stringResource(R.string.advice_same, label) else label, style = MaterialTheme.typography.titleMedium, color = CmColors.text)
                if (a.reason.isNotBlank()) Text(a.reason, style = MaterialTheme.typography.bodyMedium, color = CmColors.text2)
            }
        }
        if (!same) {
            a.cost?.let { c ->
                Row(Modifier.padding(start = 54.dp, top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.WarningAmber, null, tint = CmColors.briefWarn, modifier = Modifier.size(16.dp).offset(y = 1.dp))
                    Text(stringResource(R.string.advice_cost, MasterService.tokens(c)), style = MaterialTheme.typography.bodySmall, color = CmColors.briefWarn)
                }
            }
            FilledTonalButton(
                onClick = { onUse(go) }, modifier = Modifier.padding(start = 54.dp, top = 12.dp).handCursor(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = CmColors.advice.copy(alpha = .24f), contentColor = lerp(CmColors.advice, Color.White, .7f)),
            ) { Text(stringResource(R.string.advice_use)) }
        }
    }
}

/** «MODELLO», «EFFORT»: come i gruppi di «Vai a». */
@Composable
private fun SectionLabel(text: String) = Text(
    text.uppercase(), style = MonoSmall.copy(color = CmColors.text2), modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp),
)

/** Una voce come quelle del menu dell'app: cerchio con l'iniziale, nome, riga che lo spiega; la scelta su fondo chiaro con ✓. */
@Composable
private fun ModelRow(label: String, sub: String?, selected: Boolean, recommended: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).background(if (selected) CmColors.surfaceHigh else Color.Transparent).handCursor()
            .selectable(selected, role = Role.RadioButton, onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (selected) CmColors.accent.copy(alpha = .25f) else CmColors.surfaceHigh), contentAlignment = Alignment.Center) {
            Text(label.take(1).uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = CmColors.actionIcon)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = CmColors.text, maxLines = 1, overflow = TextOverflow.Clip)
                if (recommended) AdviceTag()
            }
            sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CmColors.text2, maxLines = 1, overflow = TextOverflow.Clip) }
        }
        if (selected) Icon(Icons.Rounded.Check, null, tint = CmColors.actionIcon)
    }
}

/**
 * L'effort come gruppo di tasti connessi di Material 3 Expressive: la scelta si arrotonda tutta; il puntino lilla segna
 * quello consigliato, se è diverso. I colori dal tema: fuori scelta il tono più alto del pannello.
 */
@Composable
private fun EffortGroup(efforts: List<String>, effort: String?, recommended: String?, onPick: (String) -> Unit) {
    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surfaceContainer = CmColors.surfaceHigh, onSurfaceVariant = CmColors.text)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
            efforts.forEachIndexed { i, e ->
                ToggleButton(
                    checked = e == effort, onCheckedChange = { onPick(e) },
                    // Larghi quanto la loro parola (Franz, 07/10 21:16: «medium» usciva dal suo tasto a larghezze uguali).
                    modifier = Modifier.weight((e.length + 2).toFloat()).heightIn(min = 44.dp).handCursor().semantics { role = Role.RadioButton },
                    shapes = when (i) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        efforts.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Box {
                        Text(e, maxLines = 1, softWrap = false)
                        if (e != effort && e == recommended) {
                            Box(Modifier.align(Alignment.TopEnd).offset(x = 8.dp, y = (-4).dp).size(7.dp).clip(CircleShape).background(CmColors.advice))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun modelSub(id: String): String? = when (Tune.kind(id)) {
    "opus" -> stringResource(R.string.model_sub_opus)
    "fable" -> stringResource(R.string.model_sub_fable)
    "sonnet" -> stringResource(R.string.model_sub_sonnet)
    "haiku" -> stringResource(R.string.model_sub_haiku)
    else -> null
}

@Composable
private fun effortSub(e: String): String? = when (e) {
    "low" -> stringResource(R.string.effort_sub_low)
    "medium" -> stringResource(R.string.effort_sub_medium)
    "high" -> stringResource(R.string.effort_sub_high)
    "xhigh" -> stringResource(R.string.effort_sub_xhigh)
    "max" -> stringResource(R.string.effort_sub_max)
    else -> null
}
