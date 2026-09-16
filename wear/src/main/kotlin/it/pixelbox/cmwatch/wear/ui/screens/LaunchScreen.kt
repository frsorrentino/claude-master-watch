package it.pixelbox.cmwatch.wear.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PlayArrow
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
import it.pixelbox.cmwatch.wear.ui.theme.morph

/**
 * «Nuova sessione» (Franz, 16/09 14:33, contratto 1.13): i progetti pubblicati dal PC, dal più usato di recente. Il tocco
 * su un progetto apre sotto di lui le due strade: scrivere il primo messaggio (tastiera o dettatura) o avviare senza.
 */
@Composable
fun LaunchScreen(projects: List<Project>, enabled: Boolean, onLaunch: (String) -> Unit, onWrite: (Project) -> Unit = {}) {
    val listState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    ScreenScaffold(scrollState = listState) { padding ->
        TransformingLazyColumn(state = listState, contentPadding = padding, modifier = Modifier.fillMaxSize()) {
            item { ListHeader(transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec)) { Text(stringResource(R.string.launch_new)) } }
            if (projects.isEmpty()) item { Text(stringResource(R.string.launch_empty), color = CmColors.text2, modifier = Modifier.morph(this, spec)) }
            for (p in LaunchRules.ordered(projects)) {
                val selected = chosen == p.path
                item {
                    WideButton(
                        "${p.name} · ${p.account}", onClick = { chosen = if (selected) null else p.path }, enabled = enabled,
                        icon = Icons.Rounded.Folder,
                        transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                    )
                }
                if (selected && LaunchRules.allowed(p.path, projects)) {
                    item {
                        WideButton(
                            stringResource(R.string.launch_with_message), onClick = { onWrite(p); chosen = null },
                            primary = true, enabled = enabled, icon = Icons.Rounded.Edit,
                            transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                        )
                    }
                    item {
                        WideButton(
                            stringResource(R.string.launch_without_message), onClick = { onLaunch(p.path); chosen = null },
                            enabled = enabled, icon = Icons.Rounded.PlayArrow,
                            transformation = SurfaceTransformation(spec), modifier = Modifier.transformedHeight(this, spec),
                        )
                    }
                }
            }
        }
    }
}
