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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.example.launcher_hyprland.data.HOME_WORKSPACE
import com.example.launcher_hyprland.data.LayoutStore
import com.example.launcher_hyprland.data.WORKSPACE_COUNT
import com.example.launcher_hyprland.data.defaultWorkspaces
import com.example.launcher_hyprland.model.Direction
import com.example.launcher_hyprland.model.TileNode
import com.example.launcher_hyprland.model.WidgetType
import com.example.launcher_hyprland.model.closeLeaf
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
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Écran d'accueil : possède tout l'état et le passe vers le bas.
 * 4 espaces de travail (un arbre chacun) dans un pager ; les commandes d'édition agissent sur l'espace affiché.
 */
@Composable
fun HomeScreen(initialEditMode: Boolean = false) {
    // L'état : chaque nouvelle valeur affectée → Compose redessine ce qui en dépend.
    var trees by remember { mutableStateOf<List<TileNode>>(defaultWorkspaces()) }
    // Une case active par espace (2 = l'horloge dans l'espace 2, comme à l'étape 3)
    var activeIds by remember { mutableStateOf(defaultWorkspaces().map { firstLeafId(it) }) }
    val pagerState = rememberPagerState(initialPage = HOME_WORKSPACE) { WORKSPACE_COUNT }
    val scope = rememberCoroutineScope()
    var editMode by remember { mutableStateOf(initialEditMode) }
    var swapFromId by remember { mutableStateOf<Int?>(null) } // case de départ d'un échange, ou null
    var pickerOpen by remember { mutableStateOf(false) }      // la liste des widgets remplace l'EditBar
    var openNoteId by remember { mutableStateOf<Int?>(null) } // case dont on édite la note, ou null

    // L'espace affiché, lu à l'appel (jamais une valeur gardée d'avant) : le balayage est coupé en mode édition.
    fun curTree(): TileNode = trees[pagerState.currentPage]
    fun curActive(): Int = activeIds[pagerState.currentPage]
    fun setTree(tree: TileNode) {
        val page = pagerState.currentPage
        trees = trees.toMutableList().also { it[page] = tree }
    }
    fun setActive(id: Int) {
        val page = pagerState.currentPage
        activeIds = activeIds.toMutableList().also { it[page] = id }
    }

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

    // Au démarrage : une seule fois (clé Unit qui ne change jamais), on relit les dispositions.
    LaunchedEffect(Unit) {
        if (store == null) return@LaunchedEffect
        store.load()?.let { saved ->
            trees = saved
            activeIds = saved.mapIndexed { i, tree ->
                val old = activeIds[i]
                if (findLeaf(tree, old) == null) firstLeafId(tree) else old
            }
        }
        loaded = true
    }

    // À chaque nouveaux arbres : on relance la tâche. Si trees rechange avant 500 ms, Compose ANNULE
    // la tâche en cours (le delay s'interrompt) et en relance une → seule la dernière écrit (debounce).
    LaunchedEffect(trees, loaded) {
        if (store == null || !loaded) return@LaunchedEffect
        delay(500.milliseconds)
        store.save(trees)
    }

    // --- les callbacks, passés vers le bas ---
    // 2e toucher sur la case déjà active (hors mode édition) : l'action du widget.
    // Arrive ici tout toucher qui n'est pas capté par une zone cliquable du widget (voir widgetClickable).
    fun onWidgetAction(id: Int) {
        when (findLeaf(curTree(), id)?.widget) {
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
            from == null && id == curActive() && !editMode -> onWidgetAction(id)
            from == null -> setActive(id)                    // cas normal : on sélectionne
            id == from -> swapFromId = null                  // retoucher la case de départ : annule
            else -> {                                        // une autre case : on échange
                setTree(swapLeaves(curTree(), from, id))
                swapFromId = null
                // la case active (= celle de départ) a juste changé de place
            }
        }
    }

    fun onSplit(direction: Direction) {
        swapFromId = null
        val tree = curTree()
        val splitId = nextId(tree)
        val newLeafId = splitId + 1
        setTree(splitLeaf(tree, curActive(), direction, splitId, newLeafId))
        setActive(newLeafId) // la nouvelle case devient active
    }

    fun onClose() {
        val tree = curTree()
        if (leafCount(tree) <= 1) return
        swapFromId = null
        // la voisine qui prend la place devient active (sa première case si c'est une découpe)
        val sibling = siblingOf(tree, curActive())
        val closed = closeLeaf(tree, curActive())
        setTree(closed)
        setActive(if (sibling != null) firstLeafId(sibling) else firstLeafId(closed))
    }

    fun onSwap() {
        // 1er appui : la case active devient le départ ; 2e appui : annule
        swapFromId = if (swapFromId == null) curActive() else null
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

    val shownPage = pagerState.currentPage
    val shownTree = trees[shownPage]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(LauncherStyle.gap),
    ) {
        HorizontalPager(
            state = pagerState,
            // Le balayage est coupé en mode édition : il entrerait en conflit avec les poignées.
            userScrollEnabled = !editMode,
            pageSpacing = LauncherStyle.gap,
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) { page ->
            val current = page == shownPage
            TilingLayout(
                tree = trees[page],
                activeId = activeIds[page],
                editMode = editMode && current,
                swapFromId = if (current) swapFromId else null,
                onTileClick = { id -> onTileClick(id) },
                onTileLongClick = { id ->
                    swapFromId = null
                    setActive(id)
                    editMode = true
                },
                // On applique le delta à l'arbre ACTUEL (jamais à un vieux ratio gardé par la poignée).
                onResize = { splitId, delta -> setTree(resize(curTree(), splitId, delta)) },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (editMode && pickerOpen) {
            Spacer(Modifier.height(LauncherStyle.gap))
            WidgetPicker(
                current = findLeaf(shownTree, activeIds[shownPage])?.widget,
                onPick = { type ->
                    setTree(setWidget(curTree(), curActive(), type)) // nouvel arbre → redessin + sauvegarde (debounce)
                    pickerOpen = false
                },
                onCancel = { pickerOpen = false },
            )
        } else if (editMode) {
            Spacer(Modifier.height(LauncherStyle.gap))
            EditBar(
                canClose = leafCount(shownTree) > 1,
                canSwap = leafCount(shownTree) > 1,
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
        Spacer(Modifier.height(LauncherStyle.gap))
        WorkspaceBar(
            count = WORKSPACE_COUNT,
            current = shownPage,
            enabled = !editMode,
            onSelect = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
        )
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
