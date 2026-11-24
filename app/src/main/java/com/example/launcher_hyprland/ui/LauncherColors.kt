package com.example.launcher_hyprland.ui

import android.content.Context
import androidx.annotation.ColorRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/**
 * La palette du launcher, lue dans les couleurs système (Material You, donc dérivées du fond d'écran).
 * Les widgets ne connaissent que ces rôles : jamais de couleur en dur.
 */
@Immutable
data class LauncherColors(
    val tile: Color,      // fond d'une case
    val border: Color,    // contour d'une case inactive, traits fins
    val text: Color,
    val muted: Color,     // texte atténué
    val accent: Color,    // case active, « maintenant », aujourd'hui, pilule active, curseur
    val accent2: Color,   // points des jours, swap, batterie
    val field: Color,     // fond des pilules / de la sélection
    val onAccent: Color,  // texte posé sur l'accent
)

/** Secours (aperçu Android Studio, où les couleurs système n'existent pas) : les anciennes valeurs de [LauncherStyle]. */
private val fallbackColors = LauncherColors(
    tile = LauncherStyle.fallbackTile,
    border = LauncherStyle.fallbackBorder,
    text = LauncherStyle.fallbackText,
    muted = LauncherStyle.fallbackMuted,
    accent = LauncherStyle.fallbackAccent,
    accent2 = LauncherStyle.fallbackAccent2,
    field = LauncherStyle.fallbackField,
    onAccent = LauncherStyle.fallbackOnAccent,
)

val LocalLauncherColors = staticCompositionLocalOf { fallbackColors }

object LauncherTheme {
    val colors: LauncherColors
        @Composable @ReadOnlyComposable get() = LocalLauncherColors.current
}

/**
 * Fournit la palette à tout ce qu'il contient. [refreshKey] : on le change pour forcer la relecture
 * des couleurs système (changement de fond d'écran sans recréation de l'activité).
 */
@Composable
fun LauncherTheme(refreshKey: Int = 0, content: @Composable () -> Unit) {
    val colors = if (LocalInspectionMode.current) {
        fallbackColors
    } else {
        val context = LocalContext.current
        remember(refreshKey, LauncherStyle.lightTiles) { readSystemColors(context, LauncherStyle.lightTiles) }
    }
    CompositionLocalProvider(LocalLauncherColors provides colors, content = content)
}

private fun readSystemColors(context: Context, light: Boolean): LauncherColors {
    fun c(@ColorRes id: Int, alpha: Float = 1f) = Color(context.getColor(id)).copy(alpha = alpha)
    return if (light) {
        LauncherColors(
            tile = c(android.R.color.system_neutral1_50, 0.92f),
            border = c(android.R.color.system_neutral2_200),
            text = c(android.R.color.system_neutral1_900),
            muted = c(android.R.color.system_neutral2_600),
            accent = c(android.R.color.system_accent1_600),
            accent2 = c(android.R.color.system_accent3_600),
            field = c(android.R.color.system_neutral1_100),
            onAccent = c(android.R.color.system_neutral1_50),
        )
    } else {
        LauncherColors(
            tile = c(android.R.color.system_neutral1_900, 0.90f),
            border = c(android.R.color.system_neutral2_700),
            text = c(android.R.color.system_neutral1_100),
            muted = c(android.R.color.system_neutral2_300),
            accent = c(android.R.color.system_accent1_200),
            accent2 = c(android.R.color.system_accent3_200),
            field = c(android.R.color.system_neutral1_800),
            onAccent = c(android.R.color.system_neutral1_900),
        )
    }
}
