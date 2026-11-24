package com.example.launcher_hyprland.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.example.launcher_hyprland.ui.widgets.agenda.hasCalendarPermission
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
import com.example.launcher_hyprland.model.WidgetType
import com.example.launcher_hyprland.model.closeLeaf
import com.example.launcher_hyprland.model.defaultTree
import com.example.launcher_hyprland.model.findLeaf
import com.example.launcher_hyprland.model.firstLeafId
import com.example.launcher_hyprland.model.leafCount
import com.example.launcher_hyprland.model.nextId
import com.example.launcher_hyprland.model.resize
import com.example.launcher_hyprland.model.setWidget
import com.example.launcher_hyprland.model.siblingOf
import com.example.launcher_hyprland.model.splitLeaf
import com.example.launcher_hyprland.model.swapLeaves
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/** Écran d'accueil : possède tout l'état et le passe vers le bas. */
@Composable
fun HomeScreen(initialEditMode: Boolean = false) {
    // L'état : chaque nouvelle valeur affectée → Compose redessine ce qui en dépend.
    var tree by remember { mutableStateOf<TileNode>(defaultTree()) }
    var activeId by remember { mutableIntStateOf(2) } // 2 = l'horloge, comme à l'étape 3
    var editMode by remember { mutableStateOf(initialEditMode) }
    var swapFromId by remember { mutableStateOf<Int?>(null) } // case de départ d'un échange, ou null
    var pickerOpen by remember { mutableStateOf(false) }      // la liste des widgets remplace l'EditBar
    var openNoteId by remember { mutableStateOf<Int?>(null) } // case dont on édite la note, ou null

    // --- sauvegarde (pas dans l'aperçu d'Android Studio : il n'a pas de vrai stockage) ---
    val context = LocalContext.current
    val store = if (LocalInspectionMode.current) null else remember { LayoutStore(context.applicationContext) }
    var loaded by remember { mutableStateOf(store == null) } // tant que false : ne rien sauvegarder

    // Boîte système de permission agenda. Si Android ne la montre plus (refus définitif), le résultat
    // arrive tout de suite « refusé » et sans justification possible : on ouvre alors les Paramètres.
    // AGENDA ouvre l'app Agenda une fois la permission accordée ; CALENDAR reste sur l'accueil.
    var openAgendaOnGrant by remember { mutableStateOf(false) }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { if (openAgendaOnGrant) openCalendarApp(context) }
        else if (!canShowCalendarDialog(context)) openAppSettings(context)
    }

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
        delay(500.milliseconds)
        store.save(tree)
    }

    // --- les callbacks, passés vers le bas ---
    // 2e toucher sur la case déjà active (hors mode édition) : l'action du widget.
    // Arrive ici tout toucher qui n'est pas capté par une zone cliquable du widget (voir widgetClickable).
    fun onWidgetAction(id: Int) {
        when (findLeaf(tree, id)?.widget) {
            WidgetType.NOTES -> openNoteId = id
            WidgetType.AGENDA ->
                if (hasCalendarPermission(context)) openCalendarApp(context)
                else {
                    openAgendaOnGrant = true
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            WidgetType.CALENDAR ->
                if (!hasCalendarPermission(context)) {
                    openAgendaOnGrant = false
                    calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            else -> Unit // les autres widgets n'ont pas (encore) d'action
        }
    }

    fun onTileClick(id: Int) {
        val from = swapFromId
        when {
            from == null && id == activeId && !editMode -> onWidgetAction(id)
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
        pickerOpen = false
    }

    // Actif seulement en mode édition : il passe alors devant celui de MainActivity.
    // Si la liste des widgets est ouverte, retour la ferme d'abord (on reste en mode édition).
    BackHandler(enabled = editMode) {
        if (pickerOpen) pickerOpen = false else exitEditMode()
    }

    // Tant que la sauvegarde n'est pas relue (quelques ms), on n'affiche rien : sinon on verrait
    // l'arbre par défaut clignoter avant d'être remplacé par le tien.
    if (!loaded) return

    // Éditeur de note : il remplace toute la disposition (et son BackHandler passe devant celui d'au-dessus).
    val noteId = openNoteId
    if (noteId != null) {
        NoteEditor(leafId = noteId, onClose = { openNoteId = null })
        return
    }

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
        if (editMode && pickerOpen) {
            Spacer(Modifier.height(LauncherStyle.gap))
            WidgetPicker(
                current = findLeaf(tree, activeId)?.widget,
                onPick = { type ->
                    tree = setWidget(tree, activeId, type) // nouvel arbre → redessin + sauvegarde (debounce)
                    pickerOpen = false
                },
                onCancel = { pickerOpen = false },
            )
        } else if (editMode) {
            Spacer(Modifier.height(LauncherStyle.gap))
            EditBar(
                canClose = leafCount(tree) > 1,
                canSwap = leafCount(tree) > 1,
                swapPending = swapFromId != null,
                onSplitRow = { onSplit(Direction.ROW) },
                onSplitColumn = { onSplit(Direction.COLUMN) },
                onClose = { onClose() },
                onSwap = { onSwap() },
                onWidget = {
                    swapFromId = null
                    pickerOpen = true
                },
                onOk = { exitEditMode() },
            )
        }
    }
}

private fun openCalendarApp(context: Context) {
    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALENDAR)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // pas d'app Agenda installée : rien à ouvrir
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}

/** Vrai si Android peut encore afficher la boîte (faux après un refus définitif). */
private fun canShowCalendarDialog(context: Context): Boolean {
    val activity = generateSequence(context) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>().firstOrNull() ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALENDAR)
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
