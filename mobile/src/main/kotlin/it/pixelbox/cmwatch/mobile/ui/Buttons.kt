package it.pixelbox.cmwatch.mobile.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/*
 * I tasti di Material con la mano del mouse (Franz, 05/10 12:38: «molti tasti non cambiano il cursore in mano»): stessi nomi
 * e stessi parametri, nello stesso package delle schermate, così ogni tasto la prende senza toccare le chiamate.
 */

@Composable
fun IconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, colors: IconButtonColors = IconButtonDefaults.iconButtonColors(), interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) =
    androidx.compose.material3.IconButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, colors = colors, interactionSource = interactionSource, content = content)

@Composable
fun FilledIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = IconButtonDefaults.filledShape, colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(), interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) =
    androidx.compose.material3.FilledIconButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, interactionSource = interactionSource, content = content)

@Composable
fun FilledTonalIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = IconButtonDefaults.filledShape, colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(), interactionSource: MutableInteractionSource? = null, content: @Composable () -> Unit) =
    androidx.compose.material3.FilledTonalIconButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, interactionSource = interactionSource, content = content)

@Composable
fun TextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.textShape, colors: ButtonColors = ButtonDefaults.textButtonColors(), contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding, interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) =
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource, content = content)

@Composable
fun Button(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.shape, colors: ButtonColors = ButtonDefaults.buttonColors(), contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) =
    androidx.compose.material3.Button(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource, content = content)

@Composable
fun FilledTonalButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.filledTonalShape, colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(), contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) =
    androidx.compose.material3.FilledTonalButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource, content = content)

@Composable
fun OutlinedButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, shape: Shape = ButtonDefaults.outlinedShape, colors: ButtonColors = ButtonDefaults.outlinedButtonColors(), contentPadding: PaddingValues = ButtonDefaults.ContentPadding, interactionSource: MutableInteractionSource? = null, content: @Composable RowScope.() -> Unit) =
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = modifier.handCursor(), enabled = enabled, shape = shape, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource, content = content)

@Composable
fun DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null, trailingIcon: (@Composable () -> Unit)? = null, enabled: Boolean = true, colors: MenuItemColors = MenuDefaults.itemColors(), contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding, interactionSource: MutableInteractionSource? = null) =
    androidx.compose.material3.DropdownMenuItem(text = text, onClick = onClick, modifier = modifier.handCursor(), leadingIcon = leadingIcon, trailingIcon = trailingIcon, enabled = enabled, colors = colors, contentPadding = contentPadding, interactionSource = interactionSource)
