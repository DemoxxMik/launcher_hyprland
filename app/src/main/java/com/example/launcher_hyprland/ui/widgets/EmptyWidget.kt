package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.launcher_hyprland.ui.LauncherStyle

/** Case sans widget : rappelle comment en choisir un. */
@Composable
fun EmptyWidget() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MonoText("vide · appui long › widget", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
    }
}
