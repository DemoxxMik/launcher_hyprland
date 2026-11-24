package com.example.launcher_hyprland.ui.widgets.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher_hyprland.data.CalendarEvent
import com.example.launcher_hyprland.data.CalendarSource
import com.example.launcher_hyprland.data.ClockSource
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.LauncherTheme
import com.example.launcher_hyprland.ui.widgets.MonoText
import com.example.launcher_hyprland.ui.widgets.widgetClickable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val monthTitleFormat = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.FRENCH)

/** Premier lundi de la grille du mois (peut tomber dans le mois précédent). */
internal fun monthGridStart(day: LocalDate): LocalDate =
    day.withDayOfMonth(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Lendemain du dernier dimanche de la grille du mois (fin exclusive). */
internal fun monthGridEnd(day: LocalDate): LocalDate =
    day.with(TemporalAdjusters.lastDayOfMonth()).with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).plusDays(1)

internal fun weekStart(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * Calendrier (prévu plein écran) : vue mois ou semaine. Une seule requête events() pour la plage affichée.
 * [interactive] = case active et hors édition : sinon aucune zone cliquable, le toucher va à la case.
 */
@Composable
fun CalendarPageWidget(interactive: Boolean, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val granted = rememberCalendarPermission()
    val now by remember { ClockSource.minuteTicks() }.collectAsStateWithLifecycle(initialValue = LocalDateTime.now())
    val today = now.toLocalDate()

    // Sauvegardés (rotation, retour sur l'accueil) : un Boolean et un Long passent tels quels dans un Bundle.
    var monthMode by rememberSaveable { mutableStateOf(true) }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpochDay)

    if (!granted) {
        MonoText("calendrier · touche 2× pour autoriser", color = LauncherTheme.colors.muted, size = LauncherStyle.textSmall)
        return
    }

    val from = if (monthMode) monthGridStart(selected) else weekStart(selected)
    val to = if (monthMode) monthGridEnd(selected) else weekStart(selected).plusDays(7)
    // Nouvelle plage (autre mois, autre semaine, changement de vue) → nouvelle requête, l'ancienne est annulée.
    val events by remember(from, to) {
        CalendarSource(context).events(from.atStartOfDay(), to.atStartOfDay())
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    CalendarPageContent(
        monthMode = monthMode,
        selected = selected,
        now = now,
        events = events,
        interactive = interactive,
        onLongClick = onLongClick,
        onMode = { monthMode = it },
        onSelect = { selectedEpochDay = it.toEpochDay() },
    )
}

@Composable
private fun CalendarPageContent(
    monthMode: Boolean,
    selected: LocalDate,
    now: LocalDateTime,
    events: List<CalendarEvent>,
    interactive: Boolean,
    onLongClick: () -> Unit,
    onMode: (Boolean) -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val title =
                if (monthMode) selected.format(monthTitleFormat)
                else "semaine ${selected.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"
            Box(Modifier.weight(1f)) {
                MonoText(title, size = LauncherStyle.textTitle, weight = FontWeight.Bold, maxLines = 1)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(LauncherStyle.pillGap)) {
                Pill("semaine", !monthMode, interactive, onLongClick) { onMode(false) }
                Pill("mois", monthMode, interactive, onLongClick) { onMode(true) }
            }
        }
        if (monthMode) {
            MonthGrid(
                selected = selected, now = now, events = events,
                interactive = interactive, onLongClick = onLongClick, onSelect = onSelect,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            WeekGrid(
                selected = selected, now = now, events = events,
                interactive = interactive, onLongClick = onLongClick, onSelect = onSelect,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

/** Pilule : choisie = remplie en accent, l'autre = contour accent. */
@Composable
private fun Pill(text: String, chosen: Boolean, interactive: Boolean, onLongClick: () -> Unit, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    val shape = RoundedCornerShape(percent = 50)
    // choisie = remplie d'accent ; l'autre = fond « champ » (maquette : .mini-seg)
    val look =
        if (chosen) Modifier.background(colors.accent, shape)
        else Modifier.background(colors.field, shape)
    Box(
        look
            .clip(shape)
            .widgetClickable(interactive, onLongClick, onClick)
            .padding(horizontal = LauncherStyle.pillPaddingH, vertical = LauncherStyle.pillPaddingV),
    ) {
        MonoText(
            text,
            color = if (chosen) colors.onAccent else colors.muted,
            size = LauncherStyle.textSmall,
        )
    }
}

// --- aperçus : événements factices ---

private val previewNow = LocalDate.of(2026, 11, 24).atTime(16, 43)

private fun fakeEvent(id: Long, title: String, day: Int, h: Int, m: Int, hours: Long, allDay: Boolean = false): CalendarEvent {
    val start = LocalDate.of(2026, 11, day).let { if (allDay) it.atStartOfDay() else it.atTime(h, m) }
    return CalendarEvent(id, title, "", start, if (allDay) start.plusDays(1) else start.plusHours(hours), allDay)
}

private val previewEvents = listOf(
    fakeEvent(1, "férié", 23, 0, 0, 0, allDay = true),
    fakeEvent(2, "atelier scéno", 23, 10, 0, 2),
    fakeEvent(3, "rdv jury", 24, 14, 30, 1),
    fakeEvent(4, "café Léa", 24, 17, 0, 1),
    fakeEvent(5, "appel", 24, 17, 30, 1),
    fakeEvent(6, "répét. install", 25, 19, 0, 3),
    fakeEvent(7, "concert", 26, 18, 0, 2),
    fakeEvent(8, "marché", 28, 7, 0, 3),
)

@Preview(widthDp = 380, heightDp = 640, showBackground = true, backgroundColor = 0xFF1E1E2E)
@Composable
private fun CalendarMonthPreview() {
    Box(Modifier.padding(LauncherStyle.tilePadding)) {
        CalendarPageContent(true, previewNow.toLocalDate(), previewNow, previewEvents, true, {}, {}, {})
    }
}

@Preview(widthDp = 380, heightDp = 640, showBackground = true, backgroundColor = 0xFF1E1E2E)
@Composable
private fun CalendarWeekPreview() {
    Box(Modifier.padding(LauncherStyle.tilePadding)) {
        CalendarPageContent(false, previewNow.toLocalDate(), previewNow, previewEvents, true, {}, {}, {})
    }
}
