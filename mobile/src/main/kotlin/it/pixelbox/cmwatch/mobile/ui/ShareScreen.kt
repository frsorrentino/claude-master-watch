package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.contract.SessionState
import it.pixelbox.cmwatch.contract.State
import it.pixelbox.cmwatch.mobile.R
import it.pixelbox.cmwatch.rules.Accounts
import it.pixelbox.cmwatch.rules.PhoneBoard
import it.pixelbox.cmwatch.ui.tokens.CmColors

/** «Condividi» (design 29/09, schermata 6): la sessione viva a cui mandare, il messaggio, un solo bottone pieno «Manda». */
@Composable
fun ShareScreen(state: State, text: String, hasImage: Boolean, sending: Boolean, onSend: (session: String, message: String) -> Unit) {
    val alive = PhoneBoard.sections(state).filter { it.group != PhoneBoard.Group.CLOSED }.flatMap { it.sessions }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf(text) }
    Column(Modifier.fillMaxSize().background(CmColors.bg).systemBarsPadding()) {
        Column(Modifier.weight(1f).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.share_title), style = MaterialTheme.typography.headlineSmall, color = CmColors.text)
            if (state.share == null) {
                // Contratto 1.19: senza `share` il relay non accetta ancora «Condividi».
                Text(stringResource(R.string.share_update_pc), color = CmColors.text2, style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            if (hasImage) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Image, null, tint = CmColors.actionIcon)
                Text(stringResource(R.string.share_with_image), color = CmColors.text2)
            }
            OutlinedTextField(message, { message = it }, label = { Text(stringResource(R.string.share_message)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(alive, key = { it.id }) { s ->
                    val on = s.name == chosen
                    Row(
                        Modifier.fillMaxWidth().background(if (on) CmColors.surfaceHigh else CmColors.surface, RoundedCornerShape(14.dp))
                            .clickable { chosen = s.name }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        AccountDot(Accounts.isPersonal(s))
                        Text(stateGlyph(s.state), color = stateColor(s.state))
                        Text(s.name, color = if (on) CmColors.actionIcon else CmColors.text, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
        if (state.share != null) Button(
            onClick = { chosen?.let { onSend(it, message.trim()) } },
            enabled = chosen != null && !sending && (hasImage || message.isNotBlank()),
            colors = ButtonDefaults.buttonColors(containerColor = CmColors.primary, contentColor = CmColors.onPrimary),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(56.dp),
        ) { Text(stringResource(R.string.share_send)) }
    }
}
