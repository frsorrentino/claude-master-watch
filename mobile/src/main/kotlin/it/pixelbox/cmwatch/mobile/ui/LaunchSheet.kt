package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.Project
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.LaunchSuggest
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** Il contenuto del foglio «Lancia» (design 29/09, schermata 4); il foglio stesso lo apre MainActivity. */
@Composable
fun LaunchSheet(state: State, onLaunch: (project: Project, firstMessage: String) -> Unit) {
    val accounts = remember(state) {
        (state.quota.keys + state.projects.map { it.account }).distinct()
            .sortedWith(compareBy<String> { a -> !(state.quota[a]?.let { Accounts.isPersonalQuota(a, it) } ?: Accounts.personal(a, null)) }.thenBy { it })
    }
    var account by rememberSaveable { mutableStateOf(accounts.firstOrNull().orEmpty()) }
    var typed by rememberSaveable { mutableStateOf("") }
    var chosen by remember { mutableStateOf<Project?>(null) }
    var first by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (accounts.size > 1) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                accounts.forEachIndexed { i, a ->
                    SegmentedButton(selected = a == account, onClick = { account = a; chosen = null }, shape = SegmentedButtonDefaults.itemShape(i, accounts.size)) { Text(a) }
                }
            }
        }
        OutlinedTextField(typed, { typed = it; chosen = null }, label = { Text(stringResource(R.string.launch_project)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        LaunchSuggest.projects(state, account, if (chosen != null) "" else typed).forEach { p ->
            Text(
                p.name, color = if (p == chosen) CmColors.actionIcon else CmColors.text, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth().clickable { chosen = p; typed = p.name }.padding(vertical = 10.dp),
            )
        }
        OutlinedTextField(first, { first = it }, label = { Text(stringResource(R.string.launch_first)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = { chosen?.let { onLaunch(it, first.trim()) } }, enabled = chosen != null,
            colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.launch)) }
    }
}
