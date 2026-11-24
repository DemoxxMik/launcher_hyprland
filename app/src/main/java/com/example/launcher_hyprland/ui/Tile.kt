package com.example.launcher_hyprland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

/** Vrai dans le contenu d'une case active : les étiquettes (TileLabel) s'en servent pour devenir une pastille. */
val LocalTileActive = compositionLocalOf { false }

/**
 * Une case réutilisable : fond arrondi, contour (accent si [active]) et contenu libre.
 * [onClick] = toucher court, [onLongClick] = appui long.
 * [swapSource] = case de départ d'un échange en attente : contour d'une autre couleur (accent2).
 */
@Composable
fun Tile(
    active: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    swapSource: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LauncherTheme.colors
    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)
    Box(
        // On part du modifier reçu (taille décidée par l'appelant), puis l'ordre compte :
        // clip → background → border → combinedClickable → padding intérieur.
        modifier = modifier
            .clip(shape)
            .background(colors.tile, shape)
            .border(
                width = LauncherStyle.borderWidth,
                color = when {
                    swapSource -> colors.accent2
                    active -> colors.accent
                    else -> colors.border
                },
                shape = shape,
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(LauncherStyle.tilePadding),
    ) {
        CompositionLocalProvider(LocalTileActive provides active) { content() }
    }
}
