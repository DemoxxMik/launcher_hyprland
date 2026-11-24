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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import com.example.launcher_hyprland.data.Contrast
import com.example.launcher_hyprland.data.LauncherSettings

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
    val shadow: Color,    // ombre pleine décalée (réglage « ombre »)
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
    shadow = LauncherStyle.fallbackShadow,
)

val LocalLauncherColors = staticCompositionLocalOf { fallbackColors }

/** Les réglages en cours (écran « réglages »). Changer la valeur fournie redessine tout ce qui les lit. */
val LocalLauncherSettings = staticCompositionLocalOf { LauncherSettings() }

/** Pour modifier les réglages depuis l'écran « réglages » (sauvegarde + redessin). Ne fait rien dans l'aperçu. */
val LocalSettingsEditor = staticCompositionLocalOf<(LauncherSettings) -> Unit> { {} }

object LauncherTheme {
    val colors: LauncherColors
        @Composable @ReadOnlyComposable get() = LocalLauncherColors.current

    val settings: LauncherSettings
        @Composable @ReadOnlyComposable get() = LocalLauncherSettings.current

    /** Couleur de l'ombre décalée des cases, ou null si le réglage « ombre » est coupé. */
    val shadow: Color?
        @Composable @ReadOnlyComposable get() =
            if (LocalLauncherSettings.current.shadow) LocalLauncherColors.current.shadow else null
}

/**
 * Fournit la palette et les réglages à tout ce qu'il contient. [refreshKey] : on le change pour forcer la relecture
 * des couleurs système (changement de fond d'écran sans recréation de l'activité).
 * L'échelle de police du réglage s'applique ici, une fois pour toutes : elle multiplie celle du téléphone,
 * donc toutes les tailles en `sp` (aucun widget à modifier).
 */
@Composable
fun LauncherTheme(
    refreshKey: Int = 0,
    settings: LauncherSettings = LauncherSettings(),
    onSettingsChange: (LauncherSettings) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val colors = if (LocalInspectionMode.current) {
        fallbackColors
    } else {
        val context = LocalContext.current
        remember(refreshKey, settings.lightTiles, settings.contrast, settings.tint) {
            readSystemColors(context, settings)
        }
    }
    val base = LocalDensity.current
    val density = remember(base, settings.fontScale) { Density(base.density, base.fontScale * settings.fontScale) }
    CompositionLocalProvider(
        LocalLauncherColors provides colors,
        LocalLauncherSettings provides settings,
        LocalSettingsEditor provides onSettingsChange,
        LocalDensity provides density,
        content = content,
    )
}

// Les tons des palettes système, du plus clair (0) au plus foncé (1000) : le contraste se règle en avançant d'un cran.
private val neutral1 = intArrayOf(
    android.R.color.system_neutral1_0, android.R.color.system_neutral1_10, android.R.color.system_neutral1_50,
    android.R.color.system_neutral1_100, android.R.color.system_neutral1_200, android.R.color.system_neutral1_300,
    android.R.color.system_neutral1_400, android.R.color.system_neutral1_500, android.R.color.system_neutral1_600,
    android.R.color.system_neutral1_700, android.R.color.system_neutral1_800, android.R.color.system_neutral1_900,
    android.R.color.system_neutral1_1000,
)
private val neutral2 = intArrayOf(
    android.R.color.system_neutral2_0, android.R.color.system_neutral2_10, android.R.color.system_neutral2_50,
    android.R.color.system_neutral2_100, android.R.color.system_neutral2_200, android.R.color.system_neutral2_300,
    android.R.color.system_neutral2_400, android.R.color.system_neutral2_500, android.R.color.system_neutral2_600,
    android.R.color.system_neutral2_700, android.R.color.system_neutral2_800, android.R.color.system_neutral2_900,
    android.R.color.system_neutral2_1000,
)
private val accent1 = intArrayOf(
    android.R.color.system_accent1_0, android.R.color.system_accent1_10, android.R.color.system_accent1_50,
    android.R.color.system_accent1_100, android.R.color.system_accent1_200, android.R.color.system_accent1_300,
    android.R.color.system_accent1_400, android.R.color.system_accent1_500, android.R.color.system_accent1_600,
    android.R.color.system_accent1_700, android.R.color.system_accent1_800, android.R.color.system_accent1_900,
    android.R.color.system_accent1_1000,
)
private val shades = intArrayOf(0, 10, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000)

/**
 * Les rôles de DESIGN.md (contraste normal, teinte 0), puis les réglages :
 * - contraste : texte, texte atténué et contours avancent d'un cran (plus loin du fond, ou plus près) ;
 * - teinte : le fond des cases et le champ se mêlent à l'accent du même ton (0 à 60 %).
 */
private fun readSystemColors(context: Context, settings: LauncherSettings): LauncherColors {
    val light = settings.lightTiles
    val shift = when (settings.contrast) {
        Contrast.SOFT -> -1
        Contrast.NORMAL -> 0
        Contrast.HIGH -> 1
    }
    val tintRatio = LauncherStyle.tintChoices[settings.tint.coerceIn(0, LauncherStyle.tintChoices.lastIndex)]

    fun c(@ColorRes id: Int, alpha: Float = 1f) = Color(context.getColor(id)).copy(alpha = alpha)
    fun tone(palette: IntArray, shade: Int, steps: Int = 0): Color {
        val i = (shades.indexOf(shade) + steps).coerceIn(0, shades.lastIndex)
        return c(palette[i])
    }
    // plus de contraste = s'éloigner du fond : vers le clair sur cases sombres, vers le foncé sur cases claires
    fun fg(palette: IntArray, shade: Int) = tone(palette, shade, if (light) shift else -shift)
    fun tinted(shade: Int, alpha: Float = 1f) =
        lerp(tone(neutral1, shade), tone(accent1, shade), tintRatio).copy(alpha = alpha)

    return if (light) {
        LauncherColors(
            tile = tinted(50, 0.92f),
            border = fg(neutral2, 200),
            text = fg(neutral1, 900),
            muted = fg(neutral2, 600),
            accent = c(android.R.color.system_accent1_600),
            accent2 = c(android.R.color.system_accent3_600),
            field = tinted(100),
            onAccent = c(android.R.color.system_neutral1_50),
            shadow = c(android.R.color.system_neutral2_400, 0.8f),
        )
    } else {
        LauncherColors(
            tile = tinted(900, 0.90f),
            border = fg(neutral2, 700),
            text = fg(neutral1, 100),
            muted = fg(neutral2, 300),
            accent = c(android.R.color.system_accent1_200),
            accent2 = c(android.R.color.system_accent3_200),
            field = tinted(800),
            onAccent = c(android.R.color.system_neutral1_900),
            shadow = c(android.R.color.system_neutral1_1000, 0.6f),
        )
    }
}
