package com.example.launcher_hyprland.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Toutes les valeurs du design au même endroit : on règle l'apparence ici, et seulement ici. */
object LauncherStyle {
    // Dimensions
    val gap = 10.dp            // écart entre les cases et avec les bords de l'écran
    val editGap = 24.dp        // écart en mode édition : assez large pour attraper la poignée au doigt
    val cornerRadius = 12.dp   // rayon des coins d'une case
    val borderWidth = 2.dp     // épaisseur du contour
    val tilePadding = 16.dp    // marge intérieure d'une case
    val handleLength = 32.dp   // longueur du petit trait visible au milieu d'une poignée
    val handleThickness = 4.dp // épaisseur de ce trait

    // Couleurs (0xAARRGGBB : AA = opacité)
    val tileBackground = Color(0xCC1E1E2E)  // fond sombre semi-transparent (~80 %)
    val borderInactive = Color(0x99585B70)  // contour discret (~60 %)
    val borderActive = Color(0xFF89B4FA)    // accent bleu clair
    val borderSwap = Color(0xFFFAB387)      // contour de la case « départ » d'un échange (pêche)
    val textPrimary = Color(0xFFCDD6F4)     // texte clair
    val textMuted = Color(0xFF7F849C)       // texte atténué pour les étiquettes
    val handleColor = Color(0xE6CDD6F4)     // trait des poignées en mode édition (~90 %)

    // Police
    val font = FontFamily.Monospace

    // Tailles de texte : les widgets n'écrivent jamais de `.sp` eux-mêmes, ils prennent l'une de celles-ci.
    val textHuge = 64.sp   // l'heure de l'horloge, le % de la batterie
    val textBody = 16.sp   // texte normal
    val textSmall = 12.sp  // étiquettes (« agenda », « notes »…)
    val lineSpacing = 8.dp // écart vertical entre deux lignes d'un widget
}
