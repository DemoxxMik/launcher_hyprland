package com.example.launcher_hyprland.ui.widgets.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.launcher_hyprland.data.CalendarEvent
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.widgets.MonoText
import com.example.launcher_hyprland.ui.widgets.widgetClickable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val dayLabelFormat = DateTimeFormatter.ofPattern("EEEE d", Locale.FRENCH)
private val weekdayInitials = listOf("L", "M", "M", "J", "V", "S", "D")

/**
 * Le mois de [selected] en 7 colonnes depuis lundi, puis la journée choisie en [DayTimeline].
 * [events] couvre toute la grille (une seule requête, faite par l'appelant).
 */
@Composable
fun MonthGrid(
    selected: LocalDate,
    now: LocalDateTime,
    events: List<CalendarEvent>,
    interactive: Boolean,
    onLongClick: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = now.toLocalDate()
    val gridStart = monthGridStart(selected)
    val weeks = (ChronoUnit.DAYS.between(gridStart, monthGridEnd(selected)) / 7).toInt()
    val busyDays = remember(events, gridStart, weeks) {
        (0 until weeks * 7).map { gridStart.plusDays(it.toLong()) }.filter { d -> events.any { it.isOn(d) } }.toSet()
    }
    val dayEvents = remember(events, selected) { events.filter { it.isOn(selected) } }

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            weekdayInitials.forEach {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    MonoText(it, color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
                }
            }
        }
        repeat(weeks) { w ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { d ->
                    val date = gridStart.plusDays(w * 7L + d)
                    DayCell(
                        date = date,
                        inMonth = date.month == selected.month,
                        isToday = date == today,
                        isSelected = date == selected,
                        hasEvents = date in busyDays,
                        modifier = Modifier
                            .weight(1f)
                            .widgetClickable(interactive, onLongClick) { onSelect(date) },
                    )
                }
            }
        }

        Spacer(Modifier.height(LauncherStyle.lineSpacing))
        Box(
            Modifier
                .fillMaxWidth()
                .height(LauncherStyle.lineWidth)
                .background(LauncherStyle.textMuted.faded(LauncherStyle.gridLineAlpha)),
        )
        Spacer(Modifier.height(LauncherStyle.lineSpacing))
        MonoText(selected.format(dayLabelFormat), color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        Spacer(Modifier.height(LauncherStyle.lineSpacing))
        if (dayEvents.isEmpty()) {
            MonoText("rien ce jour-là", color = LauncherStyle.textMuted, size = LauncherStyle.textSmall)
        } else {
            DayTimeline(events = dayEvents, day = selected, now = now, modifier = Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    hasEvents: Boolean,
    modifier: Modifier,
) {
    val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
    val fade = when {
        !inMonth -> LauncherStyle.outsideAlpha
        weekend -> LauncherStyle.dimAlpha
        else -> 1f
    }
    val selectedBorder =
        if (isSelected) Modifier.border(LauncherStyle.lineWidth, LauncherStyle.accent, RoundedCornerShape(LauncherStyle.calendarCellCorner))
        else Modifier

    Box(modifier.height(LauncherStyle.calendarCellHeight), contentAlignment = Alignment.Center) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(LauncherStyle.calendarCellInset)
                .then(selectedBorder),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val pastille =
                if (isToday) Modifier.background(LauncherStyle.accent, CircleShape) else Modifier
            Box(Modifier.size(LauncherStyle.calendarDaySize).then(pastille), contentAlignment = Alignment.Center) {
                MonoText(
                    date.dayOfMonth.toString(),
                    color = if (isToday) LauncherStyle.textOnAccent else LauncherStyle.textPrimary.faded(fade),
                    size = LauncherStyle.textSmall,
                    weight = FontWeight.Bold,
                )
            }
            val dot = Modifier.size(LauncherStyle.calendarDotSize)
            if (hasEvents) Box(dot.background(LauncherStyle.accentSecondary.faded(fade), CircleShape))
            else Box(dot)
        }
    }
}
