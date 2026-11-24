package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.example.launcher_hyprland.ui.LauncherStyle

/** Factice en attendant 5d (CalendarSource + permission). */
@Composable
fun AgendaWidget() {
    Column(verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing)) {
        MonoText("agenda", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        MonoText("09:00 cours")
        MonoText("12:30 déjeuner")
        MonoText("18:00 sport")
    }
}
