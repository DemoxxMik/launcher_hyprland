package com.example.launcher_hyprland.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.launcher_hyprland.R

/** Toutes les valeurs du design au même endroit : on règle l'apparence ici, et seulement ici. */
object LauncherStyle {
    // Dimensions
    val gap = 10.dp            // écart entre les cases et avec les bords de l'écran
    val editGap = 24.dp        // écart en mode édition : assez large pour attraper la poignée au doigt
    val cornerRadius = 8.dp   // rayon des coins d'une case
    val borderWidth = 1.dp     // épaisseur du contour
    val tilePadding = 12.dp    // marge intérieure d'une case
    val handleLength = 32.dp   // longueur du petit trait visible au milieu d'une poignée
    val handleThickness = 4.dp // épaisseur de ce trait

    // Les couleurs vivent dans LauncherColors (palette système) ; seuls les secours d'aperçu sont plus bas.

    // Épaisseurs et opacités de l'agenda (valeurs PROVISOIRES)
    val lineWidth = 1.dp           // tous les traits fins (colonne verticale, ligne « maintenant », contour des points)
    val pastAlpha = 0.4f           // multiplicateur d'opacité des événements passés
    val dimAlpha = 0.7f            // multiplicateur des éléments secondaires (colonne verticale…)
    val eventFillAlpha = 0.25f     // remplissage d'un bloc d'événement
    val eventBorderAlpha = 0.5f    // contour d'un bloc d'événement

    // Timeline d'une journée (valeurs PROVISOIRES)
    val timelineRowHeight = 44.dp   // hauteur d'une ligne d'événement (titre + lieu)
    val timelineNowHeight = 24.dp   // hauteur des lignes « maintenant », « toute la journée » et « +N »
    val timelineHourWidth = 48.dp   // colonne des heures, à gauche
    val timelineHourTop = 3.dp      // décalage de l'heure pour l'aligner sur le titre
    val timelineSpineWidth = 16.dp  // colonne de la fine ligne verticale et des points
    val timelineDotSize = 7.dp      // diamètre d'un point
    val timelineDotTop = 6.dp       // décalage du point pour l'aligner sur le titre

    // Calendrier plein écran (valeurs PROVISOIRES)
    val textTitle = 20.sp                // « novembre 2026 » / « semaine 48 »
    val textTiny = 10.sp                 // titres des blocs et heures de la vue semaine
    val outsideAlpha = 0.2f              // jours hors du mois affiché (très atténués)
    val gridLineAlpha = 0.3f             // lignes de repère et séparateurs
    val pillPaddingH = 10.dp
    val pillPaddingV = 3.dp
    val pillGap = 4.dp                   // écart entre les deux pilules
    val calendarCellHeight = 40.dp       // hauteur d'une case jour (vue mois)
    val calendarCellInset = 2.dp         // marge autour du contour du jour choisi
    val calendarCellCorner = 6.dp        // coins du contour du jour choisi
    val calendarDaySize = 26.dp          // pastille d'aujourd'hui
    val calendarDotSize = 4.dp           // point « il y a des événements »
    val weekHourWidth = 24.dp            // colonne des heures (vue semaine)
    val weekHeaderHeight = 20.dp         // ligne « lun 23 »
    val weekAllDayHeight = 16.dp         // un bloc de la bande « toute la journée »
    val weekBlockMinHeight = 12.dp       // hauteur minimale d'un bloc très court
    val weekBlockGap = 1.dp              // écart entre deux blocs voisins
    val weekBlockCorner = 3.dp
    val weekBlockPadding = 2.dp

    // Cases claires (option) : false = sombres, le defaut
    var lightTiles: Boolean = false

    // Secours de LauncherColors (apercu Android Studio uniquement) : seul endroit ou ces valeurs servent
    val fallbackTile = Color(0xE61E1E2E)
    val fallbackBorder = Color(0xFF585B70)
    val fallbackText = Color(0xFFCDD6F4)
    val fallbackMuted = Color(0xFF7F849C)
    val fallbackAccent = Color(0xFF89B4FA)
    val fallbackAccent2 = Color(0xFFFAB387)
    val fallbackField = Color(0xFF313244)
    val fallbackOnAccent = Color(0xFF1E1E2E)

    // Police : JetBrains Mono embarquée (res/font/, licence OFL dans docs/OFL.txt), 3 graisses
    val font = FontFamily(
        Font(R.font.jetbrains_mono_light, FontWeight.Light),
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    )
    val weightHuge = FontWeight.Light // l'heure : ~44 sp Light

    // Tailles de texte : les widgets n'écrivent jamais de `.sp` eux-mêmes, ils prennent l'une de celles-ci.
    val textHuge = 44.sp   // l'heure de l'horloge, le % de la batterie
    val textBody = 13.sp   // texte normal
    val textSmall = 11.sp  // étiquettes (« agenda », « notes »…)
    val lineSpacing = 8.dp // écart vertical entre deux lignes d'un widget
}
