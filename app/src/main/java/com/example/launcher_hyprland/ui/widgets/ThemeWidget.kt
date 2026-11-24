package com.example.launcher_hyprland.ui.widgets

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher_hyprland.data.ThemeResult
import com.example.launcher_hyprland.data.ThemeSettings
import com.example.launcher_hyprland.data.ThemeState
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.LauncherTheme
import kotlinx.coroutines.launch

/** Ce que l'écran du widget doit savoir ; tout le reste (écriture, sélecteur de photos) passe par les actions. */
private data class ThemeUi(
    val state: ThemeState?,      // null : réglage absent ou illisible
    val canWrite: Boolean,       // permission WRITE_SECURE_SETTINGS accordée
    val grantCommand: String,
    val hasOriginal: Boolean,    // « départ » possible
    val message: String?,        // dernier résultat (erreur, ou action en cours)
)

private class ThemeActions(
    val onStyle: (String) -> Unit = {},
    val onWallpaper: () -> Unit = {},
    val onRestore: () -> Unit = {},
)

/**
 * Widget « thème » (étape 8.7) : choisir en direct la palette (style Material You) et changer de fond
 * d'écran. Chaque appui applique tout de suite ; SystemUI régénère la palette en 1 à 2 s et le launcher
 * suit (il lit les couleurs `system_*`). La couleur de départ n'est pas proposée : celle des paramètres
 * reste. Sans la permission, le widget s'affiche, n'écrit rien et donne la commande adb.
 * Les zones ne réagissent que si la case est active ([interactive]).
 */
@Composable
fun ThemeWidget(interactive: Boolean, onLongClick: () -> Unit) {
    if (LocalInspectionMode.current) {
        // Aperçu d'Android Studio : état figé, rien n'est lu ni écrit
        ThemeContent(
            ThemeUi(
                state = ThemeState("VIBRANT", null),
                canWrite = false,
                grantCommand = "adb shell pm grant … WRITE_SECURE_SETTINGS",
                hasOriginal = true,
                message = null,
            ),
            ThemeActions(),
            interactive = false,
            onLongClick = onLongClick,
        )
        return
    }

    val context = LocalContext.current
    val theme = remember { ThemeSettings(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val state by remember { theme.changes }.collectAsStateWithLifecycle(initialValue = theme.read())
    val hasOriginal by remember { theme.hasOriginal }.collectAsStateWithLifecycle(initialValue = false)
    var message by remember { mutableStateOf<String?>(null) }

    fun run(pending: String, action: suspend () -> ThemeResult) {
        message = pending
        scope.launch {
            message = when (val result = action()) {
                ThemeResult.Ok -> null
                ThemeResult.NoPermission -> "permission manquante"
                is ThemeResult.Failed -> result.reason
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) run("fond en cours…") { theme.setWallpaper(uri) }
    }

    ThemeContent(
        ThemeUi(
            state = state,
            canWrite = theme.hasPermission(),
            grantCommand = theme.grantCommand,
            hasOriginal = hasOriginal,
            message = message,
        ),
        ThemeActions(
            onStyle = { style -> run("palette en cours…") { theme.setStyle(style) } },
            onWallpaper = {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRestore = { run("retour au départ…") { theme.restoreOriginal() } },
        ),
        interactive = interactive,
        onLongClick = onLongClick,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeContent(ui: ThemeUi, actions: ThemeActions, interactive: Boolean, onLongClick: () -> Unit) {
    val colors = LauncherTheme.colors
    val gap = Arrangement.spacedBy(6.dp)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing),
    ) {
        TileLabel("thème")

        MonoText("palette", color = colors.muted, size = LauncherStyle.textSmall, maxLines = 1)
        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
            for (style in ThemeSettings.STYLES) {
                Pill(
                    label = style.lowercase().replace('_', ' '),
                    on = style == ui.state?.style,
                    enabled = interactive,
                    onLongClick = onLongClick,
                    onClick = { actions.onStyle(style) },
                )
            }
        }

        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
            Pill("fond d'écran", on = false, enabled = interactive, onLongClick = onLongClick, onClick = actions.onWallpaper)
            if (ui.hasOriginal) {
                Pill("départ", on = false, enabled = interactive, onLongClick = onLongClick, onClick = actions.onRestore)
            }
        }

        when {
            !ui.canWrite -> {
                MonoText("permission manquante :", color = colors.accent2, size = LauncherStyle.textSmall)
                MonoText(ui.grantCommand, color = colors.muted, size = LauncherStyle.textSmall)
            }
            ui.state == null ->
                MonoText("réglage Material You absent", color = colors.accent2, size = LauncherStyle.textSmall)
        }
        ui.message?.let { MonoText(it, color = colors.muted, size = LauncherStyle.textSmall) }
    }
}

/** Pastille de choix, comme dans l'écran « réglages » : remplie d'accent si c'est le choix courant. */
@Composable
private fun Pill(label: String, on: Boolean, enabled: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (on) colors.accent else colors.field, shape)
            .widgetClickable(enabled, onLongClick, onClick)
            .padding(horizontal = LauncherStyle.pillPaddingH, vertical = 4.dp),
    ) {
        MonoText(label, color = if (on) colors.onAccent else colors.text, size = LauncherStyle.textSmall, maxLines = 1)
    }
}

@androidx.compose.ui.tooling.preview.Preview(widthDp = 220, heightDp = 200, showBackground = true, backgroundColor = 0xFF1E1E2E)
@Composable
private fun ThemeWidgetPreview() {
    LauncherTheme { Box(Modifier.fillMaxWidth().padding(12.dp)) { ThemeWidget(interactive = false, onLongClick = {}) } }
}
