package com.example.launcher_hyprland.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher_hyprland.model.WidgetType

/** Choisit le widget à dessiner selon le type stocké dans la feuille de l'arbre. */
@Composable
fun WidgetContent(type: WidgetType) {
    when (type) {
        WidgetType.CLOCK -> ClockWidget()
        WidgetType.AGENDA -> AgendaWidget()
        WidgetType.NOTES -> NotesWidget()
        WidgetType.EMPTY -> EmptyWidget()
    }
}

/** Texte maison : impose la police monospace partout. */
@Composable
fun MonoText(
    text: String,
    color: Color = LauncherStyle.textPrimary,
    size: TextUnit = 16.sp,
) {
    Text(text = text, color = color, fontSize = size, fontFamily = LauncherStyle.font)
}

@Composable
private fun ClockWidget() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
    ) {
        MonoText("14:32", size = 64.sp)
        MonoText("mardi 24 novembre", color = LauncherStyle.textMuted)
    }
}

@Composable
private fun AgendaWidget() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonoText("agenda", color = LauncherStyle.textMuted, size = 12.sp)
        MonoText("09:00 cours")
        MonoText("12:30 déjeuner")
        MonoText("18:00 sport")
    }
}

@Composable
private fun NotesWidget() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MonoText("notes", color = LauncherStyle.textMuted, size = 12.sp)
        MonoText("- acheter café")
        MonoText("- appeler maman")
        MonoText("- finir étape 3")
    }
}

@Composable
private fun EmptyWidget() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MonoText("case vide", color = LauncherStyle.textMuted)
    }
}
