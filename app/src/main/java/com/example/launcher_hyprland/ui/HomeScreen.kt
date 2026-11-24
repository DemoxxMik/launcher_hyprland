package com.example.launcher_hyprland.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val TILE_CLOCK = 0
private const val TILE_AGENDA = 1
private const val TILE_NOTES = 2

/** Écran d'accueil : horloge en haut (1/3), agenda et notes côte à côte dessous (2/3). */
@Composable
fun HomeScreen() {
    var activeTile by remember { mutableIntStateOf(TILE_CLOCK) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(LauncherStyle.gap),
        verticalArrangement = Arrangement.spacedBy(LauncherStyle.gap),
    ) {
        Tile(
            active = activeTile == TILE_CLOCK,
            onClick = { activeTile = TILE_CLOCK },
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            ClockWidget()
        }
        Row(
            modifier = Modifier.fillMaxWidth().weight(2f),
            horizontalArrangement = Arrangement.spacedBy(LauncherStyle.gap),
        ) {
            Tile(
                active = activeTile == TILE_AGENDA,
                onClick = { activeTile = TILE_AGENDA },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                AgendaWidget()
            }
            Tile(
                active = activeTile == TILE_NOTES,
                onClick = { activeTile = TILE_NOTES },
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                NotesWidget()
            }
        }
    }
}

/** Texte maison : impose la police monospace partout. */
@Composable
private fun MonoText(
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

@Preview(widthDp = 412, heightDp = 915, showBackground = true, backgroundColor = 0xFF5A6B85)
@Composable
private fun HomeScreenPreview() {
    HomeScreen()
}
