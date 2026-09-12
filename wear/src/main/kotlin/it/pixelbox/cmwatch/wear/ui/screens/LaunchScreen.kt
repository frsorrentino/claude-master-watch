package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import it.pixelbox.cmwatch.R
import it.pixelbox.cmwatch.contract.Project
import it.pixelbox.cmwatch.rules.LaunchRules
import it.pixelbox.cmwatch.wear.ui.components.WideButton
import it.pixelbox.cmwatch.wear.ui.theme.CmColors

/** Lancia: solo i progetti pubblicati dal PC; tap → il bottone pieno «Lancia nome» conferma. */
@Composable
fun LaunchScreen(projects: List<Project>, enabled: Boolean, onLaunch: (String) -> Unit) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.launch_title)) } }
            if (projects.isEmpty()) item { Text(stringResource(R.string.launch_empty), color = CmColors.text2, modifier = Modifier.transformedHeight(this, spec)) }
            for (p in projects) item {
                val selected = chosen == p.path
                WideButton(
                    if (selected) stringResource(R.string.launch_confirm, p.name) else "${p.name} · ${p.account}",
                    onClick = { if (selected && LaunchRules.allowed(p.path, projects)) { onLaunch(p.path); chosen = null } else chosen = p.path },
                    primary = selected, enabled = enabled,
                    transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                )
            }
        }
    }
}
