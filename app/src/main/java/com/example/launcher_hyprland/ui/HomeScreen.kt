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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.example.launcher_hyprland.data.LayoutStore
import com.example.launcher_hyprland.model.Direction
import com.example.launcher_hyprland.model.TileNode
import com.example.launcher_hyprland.model.closeLeaf
import com.example.launcher_hyprland.model.defaultTree
import com.example.launcher_hyprland.model.findLeaf
import com.example.launcher_hyprland.model.firstLeafId
import com.example.launcher_hyprland.model.leafCount
import com.example.launcher_hyprland.model.nextId
import com.example.launcher_hyprland.model.resize
import com.example.launcher_hyprland.model.siblingOf
import com.example.launcher_hyprland.model.splitLeaf
import com.example.launcher_hyprland.model.swapLeaves
import kotlinx.coroutines.delay

/** Écran d'accueil : possède tout l'état et le passe vers le bas. */
@Composable
fun HomeScreen(initialEditMode: Boolean = false) {
    // L'état : chaque nouvelle valeur affectée → Compose redessine ce qui en dépend.
    var tree by remember { mutableStateOf<TileNode>(defaultTree()) }
    var activeId by remember { mutableIntStateOf(2) } // 2 = l'horloge, comme à l'étape 3
    var editMode by remember { mutableStateOf(initialEditMode) }
    var swapFromId by remember { mutableStateOf<Int?>(null) } // case de départ d'un échange, ou null

    // --- sauvegarde (pas dans l'aperçu d'Android Studio : il n'a pas de vrai stockage) ---
    val context = LocalContext.current
    val store = if (LocalInspectionMode.current) null else remember { LayoutStore(context.applicationContext) }
    var loaded by remember { mutableStateOf(store == null) } // tant que false : ne rien sauvegarder

    // Au démarrage : une seule fois (clé Unit qui ne change jamais), on relit la disposition.
    LaunchedEffect(Unit) {
        if (store == null) return@LaunchedEffect
        store.load()?.let { saved ->
            tree = saved
            if (findLeaf(saved, activeId) == null) activeId = firstLeafId(saved)
        }
        loaded = true
    }

    // À chaque nouvel arbre : on relance la tâche. Si tree rechange avant 500 ms, Compose ANNULE
    // la tâche en cours (le delay s'interrompt) et en relance une → seule la dernière écrit (debounce).
    LaunchedEffect(tree, loaded) {
        if (store == null || !loaded) return@LaunchedEffect
        delay(500)
        store.save(tree)
    }

    // --- les callbacks, passés vers le bas ---
    fun onTileClick(id: Int) {
        val from = swapFromId
        when {
            from == null -> activeId = id                    // cas normal : on sélectionne
            id == from -> swapFromId = null                  // retoucher la case de départ : annule
            else -> {                                        // une autre case : on échange
                tree = swapLeaves(tree, from, id)
                swapFromId = null
                // activeId ne change pas : la case active (= celle de départ) a juste changé de place
            }
        }
    }

    fun onSplit(direction: Direction) {
        swapFromId = null
        val splitId = nextId(tree)
        val newLeafId = splitId + 1
        tree = splitLeaf(tree, activeId, direction, splitId, newLeafId)
        activeId = newLeafId // la nouvelle case devient active
    }

    fun onClose() {
        if (leafCount(tree) <= 1) return
        swapFromId = null
        // la voisine qui prend la place devient active (sa première case si c'est une découpe)
        val sibling = siblingOf(tree, activeId)
        tree = closeLeaf(tree, activeId)
        activeId = if (sibling != null) firstLeafId(sibling) else firstLeafId(tree)
    }

    fun onSwap() {
        // 1er appui : la case active devient le départ ; 2e appui : annule
        swapFromId = if (swapFromId == null) activeId else null
    }

    fun exitEditMode() {
        editMode = false
        swapFromId = null
    }

    // Actif seulement en mode édition : il passe alors devant celui de MainActivity.
    BackHandler(enabled = editMode) { exitEditMode() }

    // Tant que la sauvegarde n'est pas relue (quelques ms), on n'affiche rien : sinon on verrait
    // l'arbre par défaut clignoter avant d'être remplacé par le tien.
    if (!loaded) return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(LauncherStyle.gap),
    ) {
        TilingLayout(
            tree = tree,
            activeId = activeId,
            editMode = editMode,
            swapFromId = swapFromId,
            onTileClick = { id -> onTileClick(id) },
            onTileLongClick = { id ->
                swapFromId = null
                activeId = id
                editMode = true
            },
            // On applique le delta à l'arbre ACTUEL (jamais à un vieux ratio gardé par la poignée).
            onResize = { splitId, delta -> tree = resize(tree, splitId, delta) },
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        if (editMode) {
            Spacer(Modifier.height(LauncherStyle.gap))
            EditBar(
                canClose = leafCount(tree) > 1,
                canSwap = leafCount(tree) > 1,
                swapPending = swapFromId != null,
                onSplitRow = { onSplit(Direction.ROW) },
                onSplitColumn = { onSplit(Direction.COLUMN) },
                onClose = { onClose() },
                onSwap = { onSwap() },
                onOk = { exitEditMode() },
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
