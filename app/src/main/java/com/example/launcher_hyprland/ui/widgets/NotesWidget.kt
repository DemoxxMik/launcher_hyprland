package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher_hyprland.data.NotesStore
import com.example.launcher_hyprland.ui.LauncherStyle

private const val NOTE_LINES = 6

/**
 * Les premières lignes de la note de la case [leafId]. Pas de zone cliquable ici :
 * le toucher passe par la Tile (2e toucher sur la case active = ouvre l'éditeur, voir HomeScreen).
 */
@Composable
fun NotesWidget(leafId: Int) {
    val text = if (LocalInspectionMode.current) {
        "- acheter café\n- appeler maman" // aperçu d'Android Studio
    } else {
        val context = LocalContext.current
        // La clé leafId : si ce composable est réutilisé pour une autre case, on recrée le Flow.
        val flow = remember(leafId) { NotesStore(context.applicationContext).observe(leafId) }
        val current by flow.collectAsStateWithLifecycle(initialValue = "")
        current
    }
    NotesContent(text)
}

@Composable
private fun NotesContent(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing)) {
        MonoText("notes", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        if (text.isBlank()) {
            MonoText("touche 2× pour écrire", color = LauncherStyle.textMuted)
        } else {
            MonoText(text, maxLines = NOTE_LINES)
        }
    }
}
