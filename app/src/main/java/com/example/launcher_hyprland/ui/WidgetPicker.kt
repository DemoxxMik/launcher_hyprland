package com.example.launcher_hyprland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.launcher_hyprland.model.WidgetType
import com.example.launcher_hyprland.ui.widgets.MonoText
import com.example.launcher_hyprland.ui.widgets.label

/** L'ordre d'affichage dans la liste (indépendant de l'ordre de l'enum). */
private val pickerOrder = listOf(
    WidgetType.CLOCK,
    WidgetType.BATTERY,
    WidgetType.NOTES,
    WidgetType.AGENDA,
    WidgetType.CALENDAR,
    WidgetType.PALETTE,
    WidgetType.EMPTY,
)

/**
 * La liste des widgets, à la place de la barre d'édition. Comme EditBar, elle ne décide rien :
 * elle remonte le choix par [onPick], ou [onCancel]. Le widget actuel de la case ([current]) est en couleur.
 */
@Composable
fun WidgetPicker(
    current: WidgetType?,
    onPick: (WidgetType) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LauncherTheme.colors
    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.tile, shape)
            .border(LauncherStyle.borderWidth, colors.border, shape)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MonoText("widget de la case :", color = colors.muted, size = LauncherStyle.textSmall)
        for (type in pickerOrder) {
            BarButton(type.label(), highlighted = type == current, onClick = { onPick(type) })
        }
        BarButton("annuler", onClick = onCancel)
    }
}
