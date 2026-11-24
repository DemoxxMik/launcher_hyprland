package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.example.launcher_hyprland.ui.LauncherStyle

/** Factice en attendant 5b (la vraie source batterie). */
@Composable
fun BatteryWidget() {
    Column(verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing)) {
        MonoText("batterie", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        MonoText("--%", size = LauncherStyle.textHuge, maxLines = 1)
    }
}
