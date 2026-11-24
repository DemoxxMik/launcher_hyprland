package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher_hyprland.data.BatterySource
import com.example.launcher_hyprland.data.BatteryState
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.LauncherTheme

private const val BAR_CELLS = 10

/** Pourcentage, barre texte et état du câble. S'abonne à la batterie SEULEMENT quand l'accueil est visible. */
@Composable
fun BatteryWidget() {
    val state: BatteryState? = if (LocalInspectionMode.current) {
        BatteryState(percent = 62, plugged = true, charging = true) // aperçu d'Android Studio
    } else {
        val context = LocalContext.current
        // remember : la source et son Flow sont créés une seule fois.
        val updates = remember { BatterySource(context.applicationContext).updates() }
        // null tant que la première valeur n'est pas arrivée (quelques ms : le message est sticky).
        val current by updates.collectAsStateWithLifecycle(initialValue = null)
        current
    }
    BatteryContent(state)
}

@Composable
private fun BatteryContent(state: BatteryState?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing, Alignment.CenterVertically),
    ) {
        if (state == null) {
            MonoText("--%", weight = LauncherStyle.weightHuge, maxLines = 1, modifier = Modifier.fillMaxWidth(), autoSize = LauncherStyle.textHuge to 120.sp)
            return@Column
        }
        MonoText(
            "${state.percent}%",
            weight = LauncherStyle.weightHuge,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            autoSize = LauncherStyle.textHuge to 120.sp,
        )
        MonoText(
            bar(state.percent),
            color = if (state.charging) LauncherTheme.colors.accent2 else LauncherTheme.colors.text,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
            autoSize = LauncherStyle.textBody to 40.sp,
        )
        MonoText(
            text = when {
                state.charging -> "charge"
                state.plugged -> "secteur"   // branché mais plein : ne charge plus
                else -> "batterie"
            },
            color = LauncherTheme.colors.muted,
            maxLines = 1,
        )
    }
}

/** 62 → « ▮▮▮▮▮▮▯▯▯▯ » (arrondi à la case la plus proche). */
private fun bar(percent: Int): String {
    val filled = ((percent + 5) / 10).coerceIn(0, BAR_CELLS)
    return "▮".repeat(filled) + "▯".repeat(BAR_CELLS - filled)
}
