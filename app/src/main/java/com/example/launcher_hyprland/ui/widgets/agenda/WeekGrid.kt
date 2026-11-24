package com.example.launcher_hyprland.ui.widgets.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.example.launcher_hyprland.data.CalendarEvent
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.widgets.MonoText
import com.example.launcher_hyprland.ui.widgets.widgetClickable
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val START_HOUR = 8
private const val END_HOUR = 20
private const val STEP_HOURS = 2
private const val MIN_LAYOUT_MINUTES = 15 // un bloc très court occupe au moins ça pour le partage des colonnes
private val dayNames = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim")

/**
 * La semaine de [selected], lundi → dimanche, axe 8 h–20 h. [events] couvre les 7 jours (une seule requête,
 * faite par l'appelant). Toucher un en-tête choisit ce jour.
 */
@Composable
fun WeekGrid(
    selected: LocalDate,
    now: LocalDateTime,
    events: List<CalendarEvent>,
    interactive: Boolean,
    onLongClick: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = now.toLocalDate()
    val days = remember(selected) { weekStart(selected).let { s -> (0L..6L).map { s.plusDays(it) } } }
    val allDayByDay = remember(events, days) { days.map { d -> events.filter { it.allDay && it.isOn(d) } } }
    val gridLine = LauncherStyle.textMuted.faded(LauncherStyle.gridLineAlpha)

    Column(modifier) {
        // En-têtes « lun 23 »
        Row(Modifier.fillMaxWidth().height(LauncherStyle.weekHeaderHeight)) {
            Spacer(Modifier.width(LauncherStyle.weekHourWidth))
            days.forEachIndexed { i, d ->
                val weekend = d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY
                val color = when {
                    d == today -> LauncherStyle.accent
                    weekend -> LauncherStyle.textMuted.faded(LauncherStyle.dimAlpha)
                    else -> LauncherStyle.textMuted
                }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .widgetClickable(interactive, onLongClick) { onSelect(d) },
                    contentAlignment = Alignment.Center,
                ) {
                    MonoText(
                        "${dayNames[i]} ${d.dayOfMonth}",
                        color = color,
                        size = LauncherStyle.textTiny,
                        maxLines = 1,
                        weight = if (d == selected) FontWeight.Bold else null,
                    )
                }
            }
        }

        // Bande « toute la journée »
        if (allDayByDay.any { it.isNotEmpty() }) {
            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.width(LauncherStyle.weekHourWidth))
                allDayByDay.forEach { list ->
                    Column(Modifier.weight(1f)) {
                        list.forEach { e ->
                            EventBlock(
                                e,
                                past = e.end <= now,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(LauncherStyle.weekAllDayHeight)
                                    .padding(LauncherStyle.weekBlockGap),
                            )
                        }
                    }
                }
            }
        }

        // Grille horaire
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val hourHeight = maxHeight / (END_HOUR - START_HOUR)
            val gridWidth = maxWidth - LauncherStyle.weekHourWidth

            for (h in START_HOUR..END_HOUR step STEP_HOURS) {
                val y = (hourHeight * (h - START_HOUR)).coerceAtMost(maxHeight - LauncherStyle.lineWidth)
                Box(
                    Modifier
                        .offset(x = LauncherStyle.weekHourWidth, y = y)
                        .size(gridWidth, LauncherStyle.lineWidth)
                        .background(gridLine),
                )
                if (h < END_HOUR) {
                    Box(Modifier.offset(y = y)) {
                        MonoText(h.toString().padStart(2, '0'), color = LauncherStyle.textMuted, size = LauncherStyle.textTiny)
                    }
                }
            }

            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.width(LauncherStyle.weekHourWidth))
                days.forEach { d ->
                    DayColumn(
                        day = d,
                        events = events,
                        now = now,
                        hourHeight = hourHeight,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColumn(day: LocalDate, events: List<CalendarEvent>, now: LocalDateTime, hourHeight: Dp, modifier: Modifier) {
    val placed = remember(events, day) { layoutDay(events, day) }
    BoxWithConstraints(modifier) {
        val columnWidth = maxWidth
        placed.forEach { p ->
            val x = columnWidth * p.lane / p.lanes
            val y = hourHeight * (p.top / 60f)
            val h = maxOf(hourHeight * ((p.bottom - p.top) / 60f), LauncherStyle.weekBlockMinHeight)
            EventBlock(
                p.event,
                past = p.event.end <= now,
                modifier = Modifier
                    .offset(x = x, y = y)
                    .size(columnWidth / p.lanes, h)
                    .padding(LauncherStyle.weekBlockGap),
            )
        }
        // Ligne « maintenant » sur la colonne du jour
        if (day == now.toLocalDate()) {
            val minutes = now.hour * 60 + now.minute - START_HOUR * 60
            if (minutes in 0..(END_HOUR - START_HOUR) * 60) {
                Box(
                    Modifier
                        .offset(y = hourHeight * (minutes / 60f))
                        .fillMaxWidth()
                        .height(LauncherStyle.lineWidth)
                        .background(LauncherStyle.accent),
                )
            }
        }
    }
}

@Composable
private fun EventBlock(event: CalendarEvent, past: Boolean, modifier: Modifier) {
    val fade = if (past) LauncherStyle.pastAlpha else 1f
    val shape = RoundedCornerShape(LauncherStyle.weekBlockCorner)
    Box(
        modifier
            .clip(shape)
            .background(LauncherStyle.accent.faded(LauncherStyle.eventFillAlpha * fade), shape)
            .border(LauncherStyle.lineWidth, LauncherStyle.accent.faded(LauncherStyle.eventBorderAlpha * fade), shape)
            .padding(LauncherStyle.weekBlockPadding),
    ) {
        MonoText(
            event.title.ifBlank { "(sans titre)" },
            color = LauncherStyle.textPrimary.faded(fade),
            size = LauncherStyle.textTiny,
        )
    }
}

/** Un bloc placé dans la colonne d'un jour : minutes depuis 8 h, et sa part de la largeur (lane / lanes). */
private class Placed(val event: CalendarEvent, val top: Int, val bottom: Int) {
    val layoutBottom = maxOf(bottom, top + MIN_LAYOUT_MINUTES)
    var lane = 0
    var lanes = 1
}

/**
 * Coupe chaque événement à 8 h–20 h, puis répartit les chevauchements : dans un groupe d'événements
 * qui se chevauchent, chacun prend la première « voie » libre et le groupe partage la largeur.
 */
private fun layoutDay(events: List<CalendarEvent>, day: LocalDate): List<Placed> {
    val windowStart = day.atTime(START_HOUR, 0)
    val windowEnd = day.atTime(END_HOUR, 0)
    val items = events
        .filter { !it.allDay }
        .mapNotNull { e ->
            val s = maxOf(e.start, windowStart)
            val en = minOf(e.end, windowEnd)
            val instant = e.start == e.end && e.start >= windowStart && e.start < windowEnd
            if (en <= s && !instant) null
            else {
                val top = ChronoUnit.MINUTES.between(windowStart, s).toInt()
                Placed(e, top, maxOf(top, ChronoUnit.MINUTES.between(windowStart, en).toInt()))
            }
        }
        .sortedWith(compareBy({ it.top }, { it.bottom }))

    val group = mutableListOf<Placed>()
    val laneEnds = mutableListOf<Int>()
    var groupEnd = 0
    fun closeGroup() {
        group.forEach { it.lanes = laneEnds.size }
        group.clear()
        laneEnds.clear()
    }
    for (p in items) {
        if (group.isNotEmpty() && p.top >= groupEnd) closeGroup()
        val free = laneEnds.indexOfFirst { it <= p.top }
        if (free == -1) {
            p.lane = laneEnds.size
            laneEnds += p.layoutBottom
        } else {
            p.lane = free
            laneEnds[free] = p.layoutBottom
        }
        groupEnd = if (group.isEmpty()) p.layoutBottom else maxOf(groupEnd, p.layoutBottom)
        group += p
    }
    closeGroup()
    return items
}
