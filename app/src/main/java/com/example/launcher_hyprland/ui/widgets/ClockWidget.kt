package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher_hyprland.data.ClockSource
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.LauncherTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH)
private val dateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH) // « lun. 24 nov. »

/** L'heure et la date, mises à jour à chaque minute, SEULEMENT quand l'accueil est visible. */
@Composable
fun ClockWidget() {
    val now = if (LocalInspectionMode.current) {
        LocalDateTime.of(2026, 11, 24, 14, 32) // aperçu d'Android Studio : heure fixe
    } else {
        // remember : le Flow est créé une seule fois (pas à chaque redessin).
        val ticks = remember { ClockSource.minuteTicks() }
        // S'abonne quand l'activité passe STARTED (visible), se désabonne quand elle passe STOPPED.
        // initialValue = l'heure actuelle, pour le tout premier dessin.
        val time by ticks.collectAsStateWithLifecycle(initialValue = LocalDateTime.now())
        time
    }
    ClockContent(now)
}

@Composable
private fun ClockContent(now: LocalDateTime) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        TileLabel("horloge")
        Column(Modifier.fillMaxWidth()) {
            MonoText(
                now.format(timeFormat),
                weight = LauncherStyle.weightHuge,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                autoSize = LauncherStyle.textHuge to 120.sp,
            )
            MonoText(
                now.format(dateFormat),
                color = LauncherTheme.colors.muted,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
                autoSize = LauncherStyle.textBody to 28.sp,
            )
        }
    }
}
