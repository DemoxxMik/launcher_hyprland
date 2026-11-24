package com.example.launcher_hyprland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.launcher_hyprland.ui.widgets.MonoText

/**
 * Barre des espaces de travail : une case fine pleine largeur, une pastille par espace.
 * Active = remplie d'accent et plus large ; les autres = texte atténué. Toucher une pastille = [onSelect].
 * Appui long n'importe où sur la barre = [onLongPress] (ouvre les réglages).
 */
@Composable
fun WorkspaceBar(
    count: Int,
    current: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: () -> Unit = {},
) {
    val colors = LauncherTheme.colors
    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tileShadow(shape, LauncherTheme.shadow)
            .clip(shape)
            .background(colors.tile, shape)
            .border(LauncherStyle.borderWidth, colors.border, shape)
            .combinedClickable(enabled = enabled, onClick = {}, onLongClick = onLongPress)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val on = i == current
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = if (on) 34.dp else 18.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .then(if (on) Modifier.background(colors.accent) else Modifier)
                    .combinedClickable(enabled = enabled, onClick = { onSelect(i) }, onLongClick = onLongPress)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                MonoText(
                    text = (i + 1).toString(),
                    color = if (on) colors.onAccent else colors.muted,
                    size = LauncherStyle.textSmall,
                    maxLines = 1,
                )
            }
        }
    }
}
