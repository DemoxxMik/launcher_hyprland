package com.example.launcher_hyprland.ui.widgets.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.launcher_hyprland.data.CalendarEvent
import com.example.launcher_hyprland.data.ClockSource
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.LauncherTheme
import com.example.launcher_hyprland.ui.widgets.MonoText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val hourFormat = DateTimeFormatter.ofPattern("HH:mm")

private enum class Kind { ALL_DAY, PAST, NEXT, LATER }

private sealed interface Entry {
    data object AllDayLabel : Entry
    data class Item(val event: CalendarEvent, val kind: Kind) : Entry
    data class Now(val time: LocalDateTime) : Entry
}

private class Shown(val entries: List<Entry>, val hidden: Int)

/**
 * Une journée en liste ordonnée (pas d'échelle de temps) : heure | fine ligne avec un point | titre + lieu.
 * [now] sert aux aperçus ; sinon l'heure vient de [ClockSource]. Pas de défilement : si ça déborde,
 * on garde le dernier passé et la suite, puis « +N ».
 */
@Composable
fun DayTimeline(
    events: List<CalendarEvent>,
    day: LocalDate,
    modifier: Modifier = Modifier,
    now: LocalDateTime? = null,
) {
    val live by remember { ClockSource.minuteTicks() }.collectAsStateWithLifecycle(initialValue = LocalDateTime.now())
    val current = now ?: live
    val isToday = day == current.toLocalDate()
    val entries = remember(events, current, isToday) { buildEntries(events, current, isToday) }

    BoxWithConstraints(modifier) {
        val shown = fit(entries, maxHeight)
        Column {
            shown.entries.forEach { entry ->
                when (entry) {
                    is Entry.AllDayLabel -> LabelRow("toute la journée")
                    is Entry.Item -> EventRow(entry.event, entry.kind)
                    is Entry.Now -> NowRow(entry.time)
                }
            }
            if (shown.hidden > 0) LabelRow("+${shown.hidden}")
        }
    }
}

private fun buildEntries(events: List<CalendarEvent>, now: LocalDateTime, isToday: Boolean): List<Entry> {
    val allDay = events.filter { it.allDay }
    val timed = events.filter { !it.allDay }.sortedBy { it.start }
    val firstUpcoming = if (isToday) timed.indexOfFirst { it.end > now } else -1

    val out = mutableListOf<Entry>()
    if (allDay.isNotEmpty()) {
        out += Entry.AllDayLabel
        allDay.forEach { out += Entry.Item(it, Kind.ALL_DAY) }
    }
    timed.forEachIndexed { i, e ->
        if (isToday && i == firstUpcoming) out += Entry.Now(now)
        val kind = when {
            !isToday -> Kind.LATER
            e.end <= now -> Kind.PAST
            i == firstUpcoming -> Kind.NEXT
            else -> Kind.LATER
        }
        out += Entry.Item(e, kind)
    }
    // tout est passé : la ligne « maintenant » ferme la liste
    if (isToday && timed.isNotEmpty() && firstUpcoming == -1) out += Entry.Now(now)
    return out
}

private fun Entry.height(): Dp = when (this) {
    is Entry.Item -> LauncherStyle.timelineRowHeight
    else -> LauncherStyle.timelineNowHeight
}

private fun sum(list: List<Entry>): Dp = list.fold(0.dp) { acc, e -> acc + e.height() }

/** Choisit ce qui tient dans [avail] : tout, sinon à partir du dernier passé, avec « +N » si la fin est coupée. */
private fun fit(entries: List<Entry>, avail: Dp): Shown {
    if (sum(entries) <= avail) return Shown(entries, 0)

    val anchor = entries.indexOfLast { it is Entry.Item && it.kind == Kind.PAST }.coerceAtLeast(0)

    // La fin de la liste tient depuis l'ancre : pas de « +N », on remonte tant qu'il reste de la place.
    if (sum(entries.subList(anchor, entries.size)) <= avail) {
        var start = anchor
        while (start > 0 && sum(entries.subList(start - 1, entries.size)) <= avail) start--
        return Shown(entries.subList(start, entries.size), 0)
    }

    // Sinon on réserve une ligne pour « +N ».
    val budget = avail - LauncherStyle.timelineNowHeight
    var end = anchor
    var used = 0.dp
    while (end < entries.size && used + entries[end].height() <= budget) {
        used += entries[end].height()
        end++
    }
    if (end == anchor) end = anchor + 1 // toujours au moins une ligne
    val hidden = entries.drop(end).count { it is Entry.Item }
    return Shown(entries.subList(anchor, end), hidden)
}

internal fun Color.faded(factor: Float): Color = copy(alpha = alpha * factor)

/** Vrai si l'événement occupe une partie de [day] (fin exclusive ; un événement de durée nulle compte pour son jour). */
internal fun CalendarEvent.isOn(day: LocalDate): Boolean {
    val dayStart = day.atStartOfDay()
    val nextDay = dayStart.plusDays(1)
    return start < nextDay && (end > dayStart || start >= dayStart)
}

/** Ligne de texte alignée sur la colonne des titres (« toute la journée », « +N »). */
@Composable
private fun LabelRow(text: String) {
    Row(Modifier.fillMaxWidth().height(LauncherStyle.timelineNowHeight)) {
        Box(Modifier.width(LauncherStyle.timelineHourWidth + LauncherStyle.timelineSpineWidth))
        Box(Modifier.align(Alignment.CenterVertically)) {
            MonoText(text, color = LauncherTheme.colors.muted, size = LauncherStyle.textSmall)
        }
    }
}

@Composable
private fun NowRow(time: LocalDateTime) {
    Row(
        Modifier.fillMaxWidth().height(LauncherStyle.timelineNowHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(LauncherStyle.timelineHourWidth)) {
            MonoText(time.format(hourFormat), color = LauncherTheme.colors.accent, size = LauncherStyle.textSmall, weight = FontWeight.Bold)
        }
        Box(Modifier.weight(1f).fillMaxHeight()) {
            // la colonne verticale continue derrière, la ligne « maintenant » la traverse
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(LauncherStyle.lineWidth)
                    .background(LauncherTheme.colors.accent),
            )
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, kind: Kind) {
    val past = if (kind == Kind.PAST) LauncherStyle.pastAlpha else 1f
    val colors = LauncherTheme.colors
    val titleColor = if (kind == Kind.PAST) colors.muted else colors.text
    val mutedColor = colors.muted.faded(past)
    val spine = colors.border
    val dotColor = when (kind) {
        Kind.NEXT -> colors.accent
        Kind.PAST -> colors.border
        else -> colors.muted
    }
    val hour = if (event.allDay) "" else event.start.format(hourFormat)
    val locationLine = when {
        kind == Kind.NEXT && event.location.isNotBlank() -> "ensuite · ${event.location}"
        kind == Kind.NEXT -> "ensuite"
        else -> event.location
    }

    Row(Modifier.fillMaxWidth().height(LauncherStyle.timelineRowHeight)) {
        Box(Modifier.width(LauncherStyle.timelineHourWidth).padding(top = LauncherStyle.timelineHourTop)) {
            MonoText(hour, color = mutedColor, size = LauncherStyle.textSmall)
        }
        Box(Modifier.width(LauncherStyle.timelineSpineWidth).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(LauncherStyle.lineWidth).fillMaxHeight().background(spine))
            val dot = Modifier.padding(top = LauncherStyle.timelineDotTop).size(LauncherStyle.timelineDotSize)
            if (kind == Kind.LATER || kind == Kind.ALL_DAY) {
                // à venir : point creux (le fond est celui de la case, on le « troue » avec un contour seul)
                Box(dot.border(LauncherStyle.lineWidth, dotColor, CircleShape))
            } else {
                Box(dot.background(dotColor, CircleShape))
            }
        }
        Column(Modifier.weight(1f)) {
            MonoText(
                event.title.ifBlank { "(sans titre)" },
                color = titleColor,
                maxLines = 1,
                weight = if (kind == Kind.NEXT) FontWeight.Bold else null,
            )
            if (locationLine.isNotBlank()) {
                MonoText(locationLine, color = mutedColor, size = LauncherStyle.textSmall, maxLines = 1)
            }
        }
    }
}
