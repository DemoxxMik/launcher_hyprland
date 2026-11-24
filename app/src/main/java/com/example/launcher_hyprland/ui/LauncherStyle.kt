package com.example.launcher_hyprland.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** Toutes les valeurs du design au même endroit : on règle l'apparence ici, et seulement ici. */
object LauncherStyle {
    // Dimensions
    val gap = 10.dp            // écart entre les cases et avec les bords de l'écran
    val cornerRadius = 12.dp   // rayon des coins d'une case
    val borderWidth = 2.dp     // épaisseur du contour
    val tilePadding = 16.dp    // marge intérieure d'une case

    // Couleurs (0xAARRGGBB : AA = opacité)
    val tileBackground = Color(0xCC1E1E2E)  // fond sombre semi-transparent (~80 %)
    val borderInactive = Color(0x99585B70)  // contour discret (~60 %)
    val borderActive = Color(0xFF89B4FA)    // accent bleu clair
    val textPrimary = Color(0xFFCDD6F4)     // texte clair
    val textMuted = Color(0xFF7F849C)       // texte atténué pour les étiquettes

    // Police
    val font = FontFamily.Monospace
}
