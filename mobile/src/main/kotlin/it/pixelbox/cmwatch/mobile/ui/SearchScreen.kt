package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Event
import it.pixelbox.cmwatch.contract.SearchPage
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.ChatSearch
import it.pixelbox.cmwatch.rules.Sent
import it.pixelbox.cmwatch.ui.tokens.CmColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * La ricerca (piano 30/09, Task 5): un campo e i risultati, ognuno con sessione, ora e la riga trovata con la parte cercata
 * in grassetto. Tocco = la scheda della sessione; un evento senza sessione (il diario) apre il Diario.
 *
 * Contratto 1.27 (`remote`): il relay cerca nelle conversazioni di tutte le sessioni, vive e chiuse degli ultimi 7 giorni;
 * la richiesta parte da sola quando smetti di scrivere ([onQuery], almeno 2 caratteri), e i suoi risultati prendono il
 * posto dei messaggi mandati; restano le voci del registro. Una sessione chiusa si apre solo se il PC la conosce ancora.
 */
@Composable
fun SearchScreen(
    sent: List<Sent>, events: List<Event>, onOpen: (session: String?) -> Unit, initial: String = "",
    remote: Boolean = false, page: SearchPage? = null, loading: Boolean = false, known: Set<String> = emptySet(),
    onQuery: (String) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf(initial) }
    val local = remember(query, sent, events) { ChatSearch.find(query, sent, events) }
    val hits = remember(local, page, remote) { if (remote && page != null) ChatSearch.withConversations(local, page) else local }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // Quando smetti di scrivere: ogni ricerca costa al relay da mezzo secondo a dieci.
    LaunchedEffect(query, remote) {
        val q = query.trim()
        if (!remote || q.length < 2) return@LaunchedEffect
        kotlinx.coroutines.delay(700)
        onQuery(q)
    }
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding().imePadding()) {
        OutlinedTextField(
            query, { query = it }, singleLine = true, shape = MaterialTheme.shapes.extraLarge,
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = CmColors.text2) },
            placeholder = { Text(stringResource(if (remote) R.string.search_hint_all else R.string.search_hint)) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).focusRequester(focus),
        )
        if (loading) androidx.compose.material3.LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = CmColors.actionIcon)
        if (query.isNotBlank() && hits.isEmpty() && !loading) {
            Text(stringResource(R.string.search_none), color = CmColors.text2, modifier = Modifier.padding(horizontal = 20.dp))
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
            items(hits, key = { it.ref }) { h ->
                val canOpen = h.kind != ChatSearch.Kind.CONVERSATION || h.live == true || h.session in known
                Column(
                    Modifier.fillMaxWidth().then(if (canOpen) Modifier.handCursor().clickable { onOpen(h.session) } else Modifier).padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(h.session ?: stringResource(R.string.tab_diary), style = MaterialTheme.typography.labelLarge, color = if (canOpen) CmColors.actionIcon else CmColors.text2, maxLines = 1)
                        if (h.live == false) Text(stringResource(R.string.search_closed), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
                        Spacer(Modifier.weight(1f))
                        Text(WHEN.format(Instant.ofEpochSecond(h.at).atZone(ZoneId.systemDefault())), style = MaterialTheme.typography.labelMedium, color = CmColors.text2)
                    }
                    Text(
                        buildAnnotatedString {
                            append(h.line.substring(0, h.start))
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = CmColors.text)) { append(h.line.substring(h.start, h.end)) }
                            append(h.line.substring(h.end))
                        },
                        style = MaterialTheme.typography.bodyMedium, color = CmColors.text2,
                    )
                }
            }
            if (remote && page?.more == true) item(key = "more") {
                Text(stringResource(R.string.search_more), style = MaterialTheme.typography.bodySmall, color = CmColors.text2, modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp))
            }
        }
    }
}

private val WHEN = DateTimeFormatter.ofPattern("dd/MM HH:mm")
