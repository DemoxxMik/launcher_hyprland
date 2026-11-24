package com.example.launcher_hyprland.ui.widgets

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.example.launcher_hyprland.model.WidgetType
import com.example.launcher_hyprland.ui.LauncherStyle

/**
 * Choisit le widget à dessiner selon le type stocké dans la feuille de l'arbre.
 * [leafId] et [active] servent aux widgets qui en ont besoin (ex. les notes : « quelle note est la mienne ? »).
 * Ajouter un widget = un nouveau fichier dans ce package + une ligne ici.
 */
@Suppress("UNUSED_PARAMETER") // leafId et active serviront à partir de 5c
@Composable
fun WidgetContent(type: WidgetType, leafId: Int, active: Boolean) {
    when (type) {
        WidgetType.CLOCK -> ClockWidget()
        WidgetType.BATTERY -> BatteryWidget()
        WidgetType.NOTES -> NotesWidget()
        WidgetType.AGENDA -> AgendaWidget()
        WidgetType.EMPTY -> EmptyWidget()
    }
}

/** Le nom affiché pour chaque type (dans le choix de widget). */
fun WidgetType.label(): String = when (this) {
    WidgetType.CLOCK -> "horloge"
    WidgetType.BATTERY -> "batterie"
    WidgetType.NOTES -> "notes"
    WidgetType.AGENDA -> "agenda"
    WidgetType.EMPTY -> "vide"
}

/** Texte maison : impose la police monospace partout. Coupe avec « … » au-delà de [maxLines]. */
@Composable
fun MonoText(
    text: String,
    color: Color = LauncherStyle.textPrimary,
    size: TextUnit = LauncherStyle.textBody,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontFamily = LauncherStyle.font,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
