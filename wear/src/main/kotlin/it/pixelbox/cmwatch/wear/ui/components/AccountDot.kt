package it.pixelbox.cmwatch.wear.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/** Pallino dell'account, identico a Telegram: 🔴 agenzia, 🟢 personale. */
@Composable
fun AccountDot(account: String, modifier: Modifier = Modifier) {
    val color = if (account == "agenzia") CmColors.accountAgenzia else CmColors.accountPersonale
    Box(modifier.size(10.dp).background(color, CircleShape))
}
