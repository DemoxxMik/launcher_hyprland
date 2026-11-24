package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.example.launcher_hyprland.ui.LauncherStyle

/** Factice en attendant 5c (NotesStore + éditeur). */
@Composable
fun NotesWidget() {
    Column(verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing)) {
        MonoText("notes", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        MonoText("- acheter café")
        MonoText("- appeler maman")
        MonoText("- finir étape 5")
    }
}
