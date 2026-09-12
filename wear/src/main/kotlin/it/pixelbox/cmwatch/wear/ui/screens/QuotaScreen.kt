package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ProgressIndicatorDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.QuotaAccount
import it.pixelbox.cmwatch.rules.QuotaText
import it.pixelbox.cmwatch.wear.ui.theme.CmColors
import java.time.ZoneId

/** Quota: per ogni account un anello 5 h e la riga della settimana con il reset; grigio se il dato è vecchio. */
@Composable
fun QuotaScreen(quota: Map<String, QuotaAccount>) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val labels = QuotaText.Labels(
        week = stringResource(R.string.quota_week), reset = stringResource(R.string.quota_reset),
        stale = stringResource(R.string.quota_stale), none = stringResource(R.string.quota_none),
    )
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.quota_title)) } }
            for ((account, q) in quota) {
                item {
                    Box(Modifier.fillMaxWidth().transformedHeight(this, spec), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { QuotaText.fraction(q.h5) }, modifier = Modifier.size(96.dp), strokeWidth = 8.dp,
                            colors = ProgressIndicatorDefaults.colors(indicatorColor = if (q.stale) CmColors.stale else CmColors.accent, trackColor = CmColors.line),
                        )
                        Text(QuotaText.h5Line(account, q, labels), style = MaterialTheme.typography.bodySmall, color = CmColors.text)
                    }
                }
                item { Text(QuotaText.w7Line(q, labels, ZoneId.systemDefault()), style = MaterialTheme.typography.bodyMedium, color = if (q.stale) CmColors.stale else CmColors.text2, modifier = Modifier.fillMaxWidth().transformedHeight(this, spec)) }
            }
        }
    }
}
