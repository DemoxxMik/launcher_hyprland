package com.example.launcher_hyprland.ui.widgets

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.example.launcher_hyprland.model.WidgetType
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.LauncherTheme
import com.example.launcher_hyprland.ui.LocalTileActive
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.example.launcher_hyprland.ui.widgets.agenda.CalendarPageWidget

/**
 * Choisit le widget à dessiner selon le type stocké dans la feuille de l'arbre.
 * [leafId] et [active] servent aux widgets qui en ont besoin (ex. les notes : « quelle note est la mienne ? »).
 * Ajouter un widget = un nouveau fichier dans ce package + une ligne ici.
 * Un widget n'a de zones cliquables que si la case est active et hors édition ([widgetClickable]) ;
 * [onLongClick] = l'appui long de la case, à rebrancher sur ces zones (elles captent le geste).
 */
@Composable
fun WidgetContent(
    type: WidgetType,
    leafId: Int,
    active: Boolean,
    editMode: Boolean = false,
    onLongClick: () -> Unit = {},
) {
    val interactive = active && !editMode
    when (type) {
        WidgetType.CLOCK -> ClockWidget()
        WidgetType.BATTERY -> BatteryWidget()
        WidgetType.NOTES -> NotesWidget(leafId)
        WidgetType.AGENDA -> AgendaWidget()
        WidgetType.EMPTY -> EmptyWidget()
        WidgetType.CALENDAR -> CalendarPageWidget(interactive = interactive, onLongClick = onLongClick)
    }
}

/**
 * Zone cliquable à l'intérieur d'un widget, sans effet visuel. Si [enabled] est faux, le modificateur
 * ne fait rien : le toucher passe alors à la case (activation, mode édition…).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.widgetClickable(enabled: Boolean, onLongClick: () -> Unit, onClick: () -> Unit): Modifier =
    if (!enabled) this
    else combinedClickable(
        interactionSource = null,
        indication = null,
        onLongClick = onLongClick,
        onClick = onClick,
    )

/** Le nom affiché pour chaque type (dans le choix de widget). */
fun WidgetType.label(): String = when (this) {
    WidgetType.CLOCK -> "horloge"
    WidgetType.BATTERY -> "batterie"
    WidgetType.NOTES -> "notes"
    WidgetType.AGENDA -> "agenda"
    WidgetType.EMPTY -> "vide"
    WidgetType.CALENDAR -> "calendrier"
}

/**
 * Etiquette d'une case (« notes », « agenda »...), en minuscules. Case active : pastille remplie d'accent
 * (texte sur accent) ; sinon texte attenue, petit.
 */
@Composable
fun TileLabel(text: String) {
    val colors = LauncherTheme.colors
    if (LocalTileActive.current) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .background(colors.accent, RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp),
        ) {
            MonoText(text.lowercase(), color = colors.onAccent, size = LauncherStyle.textSmall, maxLines = 1)
        }
    } else {
        MonoText(text.lowercase(), color = colors.muted, size = LauncherStyle.textSmall, maxLines = 1)
    }
}

/** Texte maison : impose la police monospace partout. Coupe avec « … » au-delà de [maxLines]. */
@Composable
fun MonoText(
    text: String,
    color: Color = LauncherTheme.colors.text,
    size: TextUnit = LauncherStyle.textBody,
    maxLines: Int = Int.MAX_VALUE,
    weight: FontWeight? = null,
) {
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontWeight = weight,
        fontFamily = LauncherStyle.font,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
