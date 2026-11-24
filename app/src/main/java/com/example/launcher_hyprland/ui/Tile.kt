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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate

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
            .tileShadow(shape, LauncherTheme.shadow) // avant clip : l'ombre déborde de la case
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

/**
 * Ombre pleine décalée (réglage « ombre ») : la forme de la case, décalée de [LauncherStyle.shadowOffset]
 * vers la droite et le bas, dessinée seulement là où la case n'est pas (le fond de case étant transparent à 90 %,
 * on ne veut pas la voir au travers). [color] null = pas d'ombre. À placer AVANT `clip` dans la chaîne.
 */
fun Modifier.tileShadow(shape: Shape, color: Color?): Modifier {
    if (color == null) return this
    return drawBehind {
        val path = Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawBehind)) }
        val offset = LauncherStyle.shadowOffset.toPx()
        clipPath(path, ClipOp.Difference) {
            translate(offset, offset) { drawPath(path, color) }
        }
    }
}
