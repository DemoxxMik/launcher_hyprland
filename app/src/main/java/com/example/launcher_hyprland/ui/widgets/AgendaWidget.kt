package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher_hyprland.data.CalendarEvent
import com.example.launcher_hyprland.data.CalendarSource
import com.example.launcher_hyprland.data.ClockSource
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.widgets.agenda.DayTimeline
import com.example.launcher_hyprland.ui.widgets.agenda.rememberCalendarPermission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.LocalDateTime

/** Les événements d'aujourd'hui. Les données viennent de [CalendarSource] ; l'affichage est dans [AgendaContent]. */
@Composable
fun AgendaWidget() {
    val context = LocalContext.current
    val granted = rememberCalendarPermission()
    val now by remember { ClockSource.minuteTicks() }.collectAsStateWithLifecycle(initialValue = LocalDateTime.now())
    val today = now.toLocalDate()

    // Le flow est recréé si le jour change (minuit) ou si la permission change.
    val events by remember(today, granted) {
        val flow: Flow<List<CalendarEvent>> =
            if (granted) CalendarSource(context).events(today.atStartOfDay(), today.plusDays(1).atStartOfDay())
            else flowOf(emptyList())
        flow
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    AgendaContent(granted = granted, events = events, today = today)
}

/** Partie sans données ni permission : on lui passe tout, ce qui permet de la prévisualiser. */
@Composable
private fun AgendaContent(
    granted: Boolean,
    events: List<CalendarEvent>,
    today: LocalDate,
    now: LocalDateTime? = null,
) {
    if (!granted) {
        MonoText("agenda · touche 2× pour autoriser", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        return
    }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MonoText("aujourd'hui", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
            Spacer(Modifier.weight(1f))
            MonoText(events.size.toString(), color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        }
        if (events.isEmpty()) {
            MonoText("rien aujourd'hui", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        } else {
            DayTimeline(events = events, day = today, modifier = Modifier.weight(1f).fillMaxWidth(), now = now)
        }
    }
}

// --- aperçus : événements factices ---

private val previewDay = LocalDate.of(2026, 11, 24)

private fun fake(id: Long, title: String, location: String, h: Int, m: Int, durationH: Long = 1) =
    CalendarEvent(
        id = id, title = title, location = location,
        start = previewDay.atTime(h, m), end = previewDay.atTime(h, m).plusHours(durationH),
        allDay = false,
    )

private val previewEvents = listOf(
    fake(1, "atelier scéno", "", 10, 0),
    fake(2, "rdv jury", "", 14, 30),
    fake(3, "café Léa", "bar du coin", 17, 0),
    fake(4, "répét. install", "atelier 3", 19, 0),
)

@Preview(widthDp = 260, heightDp = 260, showBackground = true, backgroundColor = 0xFF1E1E2E)
@Composable
private fun AgendaPreview() {
    Column(Modifier.padding(LauncherStyle.tilePadding)) {
        AgendaContent(granted = true, events = previewEvents, today = previewDay, now = previewDay.atTime(16, 43))
    }
}

@Preview(widthDp = 260, heightDp = 160, showBackground = true, backgroundColor = 0xFF1E1E2E)
@Composable
private fun AgendaOverflowPreview() {
    Column(Modifier.padding(LauncherStyle.tilePadding)) {
        AgendaContent(granted = true, events = previewEvents, today = previewDay, now = previewDay.atTime(16, 43))
    }
}

@Preview(widthDp = 260, heightDp = 100, showBackground = true, backgroundColor = 0xFF1E1E2E)
@Composable
private fun AgendaNoPermissionPreview() {
    Column(Modifier.padding(LauncherStyle.tilePadding)) {
        AgendaContent(granted = false, events = emptyList(), today = previewDay)
    }
}

@Preview(widthDp = 260, heightDp = 100, showBackground = true, backgroundColor = 0xFF1E1E2E)
@Composable
private fun AgendaEmptyPreview() {
    Column(Modifier.padding(LauncherStyle.tilePadding)) {
        AgendaContent(granted = true, events = emptyList(), today = previewDay)
    }
}
