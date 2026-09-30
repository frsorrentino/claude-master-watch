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
import it.pixelbox.cmwatch.rules.QuotaBar
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** La regia (design 29/09, schermata 1): quota per account, card per stato, chiuse raccolte. «Lancia» è nel bottone mobile. */
@Composable
fun SessionsScreen(snapshot: Snapshot, now: Long, onOpen: (sessionId: String) -> Unit) {
    val state = snapshot.state
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularWavyProgressIndicator(color = CmColors.actionIcon) }
        return
    }
    var closedOpen by rememberSaveable { mutableStateOf(false) }
    val sections = PhoneBoard.sections(state)
    Column(Modifier.fillMaxSize().background(CmColors.bg)) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            // In fondo lo spazio del bottone mobile: l'ultima card non ci finisce sotto (revisione 30/09).
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(PhoneBoard.quotaRows(state, now, dataStale = snapshot.freshness is Freshness.Stale), key = { "q-" + it.account }) { QuotaLine(it) }
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
private fun QuotaLine(q: PhoneBoard.QuotaRow) {
    val spec = QuotaBar.of(q.pct)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccountDot(q.personal)
            Text(q.account, color = CmColors.text, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(q.pct?.let { "$it%" } ?: "", color = CmColors.text, style = MaterialTheme.typography.titleSmall)
        }
        LinearWavyProgressIndicator(
            progress = { spec.fill / 100f }, color = if ((q.pct ?: 0) >= 90) CmColors.waiting else CmColors.briefRing,
            trackColor = CmColors.briefTrack, modifier = Modifier.fillMaxWidth(),
        )
        val reset = q.resetAt
        val note = when {
            q.stale -> stringResource(R.string.quota_old)
            reset != null -> stringResource(R.string.quota_resets_at, HHMM.format(Instant.ofEpochSecond(reset).atZone(ZoneId.systemDefault())))
            else -> null
        }
        note?.let { Text(it, color = CmColors.text2, style = MaterialTheme.typography.bodySmall) }
    }
}

private val HHMM = DateTimeFormatter.ofPattern("HH:mm")
