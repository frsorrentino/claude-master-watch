@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Freshness
import it.pixelbox.cmwatch.data.Snapshot
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.mobile.ui.art.EmptySessionsScene
import it.pixelbox.cmwatch.rules.PhoneBoard
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** La regia (design 29/09, schermata 1): quota per account, card per stato, chiuse raccolte. «Lancia» è nel bottone mobile. */
@Composable
fun SessionsScreen(snapshot: Snapshot, now: Long, onOpen: (sessionId: String) -> Unit, onQuota: () -> Unit = {}) {
    val state = snapshot.state
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularWavyProgressIndicator(color = CmColors.actionIcon) }
        return
    }
    var closedOpen by rememberSaveable { mutableStateOf(false) }
    val sections = PhoneBoard.sections(state, withMaster = false)
    Column(Modifier.fillMaxSize().background(CmColors.bg)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            // In fondo lo spazio del bottone mobile: l'ultima card non ci finisce sotto (revisione 30/09).
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Una riga di chip al posto delle due barre (Franz, 01/10 22:07, variante 2): la quota ha casa nella Panoramica,
            // qui resta un rimando piccolo e toccabile.
            PhoneBoard.quotaRows(state, now, dataStale = snapshot.freshness is Freshness.Stale).takeIf { it.isNotEmpty() }?.let { rows ->
                item(key = "quota") { QuotaChips(rows, onQuota) }
            }
            (snapshot.freshness as? Freshness.Stale)?.let { st ->
                item(key = "stale") {
                    Text(
                        stringResource(R.string.stale_data, st.minutes), color = CmColors.text2, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CmColors.surfaceLow).padding(10.dp),
                    )
                }
            }
            if (sections.isEmpty()) {
                item(key = "empty") {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        EmptySessionsScene(Modifier.fillMaxWidth().height(240.dp))
                        Text(stringResource(R.string.no_sessions), color = CmColors.text2, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            for (sec in sections) {
                if (sec.group == PhoneBoard.Group.CLOSED) {
                    item(key = "closed") {
                        Text(
                            stringResource(R.string.closed_n, sec.sessions.size), color = CmColors.text2, style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.fillMaxWidth().clickable { closedOpen = !closedOpen }.padding(vertical = 10.dp),
                        )
                    }
                    if (!closedOpen) continue
                }
                items(sec.sessions, key = { it.id }) { s -> SessionCard(s, now, onClick = { onOpen(s.id) }, modifier = Modifier.animateItem()) }
            }
        }
    }
}

@Composable
private fun QuotaChips(rows: List<PhoneBoard.QuotaRow>, onClick: () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { q ->
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(CmColors.surface).clickable(onClick = onClick).padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MiniRing(q.pct, if (q.stale) CmColors.text2 else if ((q.pct ?: 0) >= 90) CmColors.waiting else CmColors.briefRing)
                Text(q.account, color = CmColors.text, style = MaterialTheme.typography.labelLarge)
                Text(
                    q.pct?.let { "$it%" } ?: "–", style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = if (q.stale) CmColors.waiting else CmColors.text2,
                )
            }
        }
    }
}

/** L'anello piccolo del chip: la finestra di 5 ore, come gli anelli della Panoramica. */
@Composable
private fun MiniRing(pct: Int?, color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.Canvas(Modifier.size(16.dp)) {
        val w = 3.dp.toPx(); val inset = w / 2
        val sz = androidx.compose.ui.geometry.Size(size.width - w, size.height - w)
        val tl = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(CmColors.briefTrack, -90f, 360f, false, topLeft = tl, size = sz, style = androidx.compose.ui.graphics.drawscope.Stroke(w))
        if (pct != null && pct > 0) drawArc(color, -90f, 360f * (pct.coerceIn(0, 100) / 100f), false, topLeft = tl, size = sz,
            style = androidx.compose.ui.graphics.drawscope.Stroke(w, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

