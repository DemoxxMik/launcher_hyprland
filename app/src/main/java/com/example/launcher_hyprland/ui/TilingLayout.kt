package com.example.launcher_hyprland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import com.example.launcher_hyprland.model.Direction
import com.example.launcher_hyprland.model.TileNode

/**
 * Dessine tout l'arbre [tree]. Ne modifie rien : il reçoit l'état ([activeId], [editMode],
 * [swapFromId]) et remonte les gestes par callbacks.
 * [onResize] reçoit l'id de la découpe et un DELTA en fraction (pas un nouveau ratio).
 */
@Composable
fun TilingLayout(
    tree: TileNode,
    activeId: Int,
    editMode: Boolean,
    swapFromId: Int?,
    onTileClick: (Int) -> Unit,
    onTileLongClick: (Int) -> Unit,
    onResize: (splitId: Int, deltaFraction: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    NodeView(
        node = tree,
        activeId = activeId,
        editMode = editMode,
        swapFromId = swapFromId,
        onTileClick = onTileClick,
        onTileLongClick = onTileLongClick,
        onResize = onResize,
        modifier = modifier.fillMaxSize(),
    )
}

/** Fonction récursive : une feuille → une Tile ; une découpe → Row/Column : enfant, poignée, enfant. */
@Composable
private fun NodeView(
    node: TileNode,
    activeId: Int,
    editMode: Boolean,
    swapFromId: Int?,
    onTileClick: (Int) -> Unit,
    onTileLongClick: (Int) -> Unit,
    onResize: (Int, Float) -> Unit,
    modifier: Modifier,
) {
    when (node) {
        is TileNode.Leaf -> key(node.id) {
            Tile(
                active = node.id == activeId,
                swapSource = node.id == swapFromId,
                onClick = { onTileClick(node.id) },
                onLongClick = { onTileLongClick(node.id) },
                modifier = modifier,
            ) {
                WidgetContent(node.widget)
            }
        }

        is TileNode.Split -> key(node.id) {
            // Taille du Row/Column parent en pixels (dans le sens de la découpe), mesurée à l'affichage.
            var parentPx by remember { mutableIntStateOf(0) }
            val handlePx = with(LocalDensity.current) {
                (if (editMode) LauncherStyle.editGap else LauncherStyle.gap).toPx()
            }
            // Pixels glissés → fraction. On divise par la place partagée par les deux enfants
            // (le parent moins la poignée) : comme ça la poignée suit exactement le doigt.
            // On n'envoie que le DELTA : c'est HomeScreen qui l'applique à l'arbre actuel.
            val onDrag: (Float) -> Unit = { deltaPx ->
                val available = parentPx - handlePx
                if (available > 0f) onResize(node.id, deltaPx / available)
            }

            // Les deux enfants, pour ne pas répéter la longue liste de paramètres.
            val child = @Composable { n: TileNode, m: Modifier ->
                NodeView(n, activeId, editMode, swapFromId, onTileClick, onTileLongClick, onResize, m)
            }

            when (node.direction) {
                Direction.ROW -> Row(modifier.onSizeChanged { parentPx = it.width }) {
                    child(node.first, Modifier.weight(node.ratio).fillMaxHeight())
                    Handle(Orientation.Horizontal, editMode, onDrag)
                    child(node.second, Modifier.weight(1f - node.ratio).fillMaxHeight())
                }

                Direction.COLUMN -> Column(modifier.onSizeChanged { parentPx = it.height }) {
                    child(node.first, Modifier.weight(node.ratio).fillMaxWidth())
                    Handle(Orientation.Vertical, editMode, onDrag)
                    child(node.second, Modifier.weight(1f - node.ratio).fillMaxWidth())
                }
            }
        }
    }
}

/**
 * L'écart entre deux enfants. Hors mode édition : un simple espace de [LauncherStyle.gap].
 * En mode édition : plus large ([LauncherStyle.editGap]), un trait au milieu, et glissable.
 * [orientation] = sens du glissé (Horizontal dans un Row, Vertical dans une Column).
 */
@Composable
private fun Handle(
    orientation: Orientation,
    editMode: Boolean,
    onDrag: (Float) -> Unit,
) {
    val horizontal = orientation == Orientation.Horizontal
    val thickness = if (editMode) LauncherStyle.editGap else LauncherStyle.gap
    val sizeModifier =
        if (horizontal) Modifier.width(thickness).fillMaxHeight()
        else Modifier.height(thickness).fillMaxWidth()

    Box(
        modifier = sizeModifier.draggable(
            state = rememberDraggableState(onDrag),
            orientation = orientation,
            enabled = editMode,
        ),
        contentAlignment = Alignment.Center,
    ) {
        if (editMode) {
            val barSize =
                if (horizontal) Modifier.size(LauncherStyle.handleThickness, LauncherStyle.handleLength)
                else Modifier.size(LauncherStyle.handleLength, LauncherStyle.handleThickness)
            Box(barSize.background(LauncherStyle.handleColor, RoundedCornerShape(50)))
        }
    }
}
