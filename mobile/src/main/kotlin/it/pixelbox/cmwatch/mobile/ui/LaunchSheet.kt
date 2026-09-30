package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Project
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.LaunchSuggest
import it.pixelbox.cmwatch.ui.tokens.CmColors

/**
 * Il contenuto del foglio «Lancia» (design 29/09, schermata 4), usato anche per «Aggiungi alla notte» (`action`). La
 * ricerca del progetto ha il completamento (Franz, 30/09 20:24): un menu attaccato al campo con i progetti di tutti e
 * due gli account in ordine `LaunchSuggest.ranked`, la parte trovata in grassetto; gli account in testa sono un filtro
 * facoltativo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchSheet(state: State, action: Int = R.string.launch, onLaunch: (project: Project, firstMessage: String) -> Unit) {
    val accounts = remember(state) {
        (state.quota.keys + state.projects.map { it.account }).distinct()
            .sortedWith(compareBy<String> { a -> !(state.quota[a]?.let { Accounts.isPersonalQuota(a, it) } ?: Accounts.personal(a, null)) }.thenBy { it })
    }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var typed by rememberSaveable { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Project?>(null) }
    var first by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val found = LaunchSuggest.ranked(state, if (chosen != null) "" else typed, filter)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(action), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold), color = CmColors.text)
        if (accounts.size > 1) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            accounts.forEach { a ->
                val personal = state.quota[a]?.let { Accounts.isPersonalQuota(a, it) } ?: Accounts.personal(a, null)
                FilterChip(
                    selected = filter == a, onClick = { filter = if (filter == a) null else a; chosen = null },
                    label = { Text(a) }, leadingIcon = { AccountMark(personal, size = 12.dp) },
                )
            }
        }
        ExposedDropdownMenuBox(expanded = menu && found.isNotEmpty(), onExpandedChange = { menu = it }) {
            OutlinedTextField(
                typed, { typed = it; chosen = null; menu = true }, label = { Text(stringResource(R.string.launch_project)) }, singleLine = true,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menu) },
                modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            )
            ExposedDropdownMenu(expanded = menu && found.isNotEmpty(), onDismissRequest = { menu = false }, containerColor = CmColors.surface) {
                found.forEach { p ->
                    val personal = state.quota[p.account]?.let { Accounts.isPersonalQuota(p.account, it) } ?: Accounts.personal(p.account, null)
                    DropdownMenuItem(
                        leadingIcon = { AccountMark(personal, size = 14.dp) },
                        text = {
                            Column {
                                Text(highlight(p.name, typed), style = MaterialTheme.typography.bodyLarge, color = CmColors.text)
                                Text(p.path.substringBeforeLast('/').substringAfterLast('/'), style = MaterialTheme.typography.bodySmall, color = CmColors.text2)
                            }
                        },
                        onClick = { chosen = p; typed = p.name; menu = false },
                    )
                }
            }
        }
        OutlinedTextField(first, { first = it }, label = { Text(stringResource(R.string.launch_first)) }, minLines = 3, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { chosen?.let { onLaunch(it, first.trim()) } }, enabled = chosen != null,
            colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(action)) }
    }
}

/** Il nome con la parte che corrisponde al testo scritto in grassetto (maiuscole e spazi ignorati, come la ricerca). */
private fun highlight(name: String, typed: String): androidx.compose.ui.text.AnnotatedString {
    val t = typed.trim()
    val i = if (t.isEmpty()) -1 else name.indexOf(t, ignoreCase = true)
    return androidx.compose.ui.text.buildAnnotatedString {
        append(name)
        if (i >= 0) addStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = CmColors.actionIcon), i, i + t.length)
    }
}
