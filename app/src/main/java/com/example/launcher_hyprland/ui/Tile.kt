package com.example.launcher_hyprland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

/**
 * Une case réutilisable : fond sombre arrondi, contour (accent si [active]) et contenu libre.
 * C'est la brique que le tiling de l'étape 4 placera dans chaque feuille de l'arbre.
 */
@Composable
fun Tile(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)
    Box(
        // On part du modifier reçu (taille décidée par l'appelant), puis l'ordre compte :
        // clip → background → border → clickable → padding intérieur.
        modifier = modifier
            .clip(shape)
            .background(LauncherStyle.tileBackground, shape)
            .border(
                width = LauncherStyle.borderWidth,
                color = if (active) LauncherStyle.borderActive else LauncherStyle.borderInactive,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(LauncherStyle.tilePadding),
        content = content,
    )
}
