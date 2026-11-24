package com.example.launcher_hyprland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

/**
 * Une case réutilisable : fond sombre arrondi, contour (accent si [active]) et contenu libre.
 * [onClick] = toucher court, [onLongClick] = appui long.
 */
@Composable
fun Tile(
    active: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)
    Box(
        // On part du modifier reçu (taille décidée par l'appelant), puis l'ordre compte :
        // clip → background → border → combinedClickable → padding intérieur.
        modifier = modifier
            .clip(shape)
            .background(LauncherStyle.tileBackground, shape)
            .border(
                width = LauncherStyle.borderWidth,
                color = if (active) LauncherStyle.borderActive else LauncherStyle.borderInactive,
                shape = shape,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(LauncherStyle.tilePadding),
        content = content,
    )
}
