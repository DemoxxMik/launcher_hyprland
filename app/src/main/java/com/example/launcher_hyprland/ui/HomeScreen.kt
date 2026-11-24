package com.example.launcher_hyprland.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.launcher_hyprland.model.Direction
import com.example.launcher_hyprland.model.TileNode
import com.example.launcher_hyprland.model.closeLeaf
import com.example.launcher_hyprland.model.defaultTree
import com.example.launcher_hyprland.model.firstLeafId
import com.example.launcher_hyprland.model.leafCount
import com.example.launcher_hyprland.model.nextId
import com.example.launcher_hyprland.model.siblingOf
import com.example.launcher_hyprland.model.splitLeaf

/** Écran d'accueil : possède tout l'état et le passe vers le bas. */
@Composable
fun HomeScreen(initialEditMode: Boolean = false) {
    // L'état : chaque nouvelle valeur affectée → Compose redessine ce qui en dépend.
    var tree by remember { mutableStateOf<TileNode>(defaultTree()) }
    var activeId by remember { mutableIntStateOf(2) } // 2 = l'horloge, comme à l'étape 3
    var editMode by remember { mutableStateOf(initialEditMode) }

    // --- les callbacks, passés vers le bas ---
    fun onSplit(direction: Direction) {
        val splitId = nextId(tree)
        val newLeafId = splitId + 1
        tree = splitLeaf(tree, activeId, direction, splitId, newLeafId)
        activeId = newLeafId // la nouvelle case devient active
    }

    fun onClose() {
        if (leafCount(tree) <= 1) return
        // la voisine qui prend la place devient active (sa première case si c'est une découpe)
        val sibling = siblingOf(tree, activeId)
        tree = closeLeaf(tree, activeId)
        activeId = if (sibling != null) firstLeafId(sibling) else firstLeafId(tree)
    }

    // Actif seulement en mode édition : il passe alors devant celui de MainActivity.
    BackHandler(enabled = editMode) { editMode = false }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(LauncherStyle.gap),
    ) {
        TilingLayout(
            tree = tree,
            activeId = activeId,
            onTileClick = { id -> activeId = id },
            onTileLongClick = { id ->
                activeId = id
                editMode = true
            },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        if (editMode) {
            Spacer(Modifier.height(LauncherStyle.gap))
            EditBar(
                canClose = leafCount(tree) > 1,
                onSplitRow = { onSplit(Direction.ROW) },
                onSplitColumn = { onSplit(Direction.COLUMN) },
                onClose = { onClose() },
                onOk = { editMode = false },
            )
        }
    }
}

@Preview(widthDp = 412, heightDp = 915, showBackground = true, backgroundColor = 0xFF5A6B85)
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}

@Preview(widthDp = 412, heightDp = 915, showBackground = true, backgroundColor = 0xFF5A6B85)
@Composable
private fun HomeScreenEditPreview() {
    HomeScreen(initialEditMode = true)
}
