package com.example.launcher_hyprland.ui.widgets

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import com.example.launcher_hyprland.ui.LauncherColors
import com.example.launcher_hyprland.ui.LauncherStyle
import com.example.launcher_hyprland.ui.LauncherTheme

/** Ce qu'Android dit du fond d'écran actuel (null : pas de réponse, ou aperçu d'Android Studio). */
private data class WallpaperInfo(
    val primary: Color?,
    val secondary: Color?,
    val tertiary: Color?,
    val hints: Int,
)

/**
 * Widget de diagnostic (étape 8.2) : les 8 rôles de la palette du launcher (pastille + hex) et les
 * couleurs que le système extrait du fond d'écran. Le compteur « changements » s'incrémente à chaque
 * événement « couleurs du fond changées » reçu pendant que le widget est affiché.
 */
@Composable
fun PaletteWidget() {
    var changes by remember { mutableIntStateOf(0) }
    val wallpaper: WallpaperInfo? = if (LocalInspectionMode.current) {
        null
    } else {
        val context = LocalContext.current
        val manager = remember { WallpaperManager.getInstance(context) }
        var info by remember { mutableStateOf(readWallpaper(manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM))) }
        DisposableEffect(manager) {
            val listener = WallpaperManager.OnColorsChangedListener { colors, which ->
                if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                    info = readWallpaper(colors ?: manager.getWallpaperColors(WallpaperManager.FLAG_SYSTEM))
                    changes++
                }
            }
            manager.addOnColorsChangedListener(listener, Handler(Looper.getMainLooper()))
            onDispose { manager.removeOnColorsChangedListener(listener) }
        }
        info
    }
    PaletteContent(LauncherTheme.colors, wallpaper, changes)
}

private fun readWallpaper(colors: WallpaperColors?): WallpaperInfo? = colors?.let {
    WallpaperInfo(
        primary = Color(it.primaryColor.toArgb()),
        secondary = it.secondaryColor?.let { c -> Color(c.toArgb()) },
        tertiary = it.tertiaryColor?.let { c -> Color(c.toArgb()) },
        hints = it.colorHints,
    )
}

@Composable
private fun PaletteContent(colors: LauncherColors, wallpaper: WallpaperInfo?, changes: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        TileLabel("palette")
        MonoText("launcher", color = colors.muted, size = LauncherStyle.textSmall)
        SwatchRow("tile", colors.tile)
        SwatchRow("border", colors.border)
        SwatchRow("text", colors.text)
        SwatchRow("muted", colors.muted)
        SwatchRow("accent", colors.accent)
        SwatchRow("accent2", colors.accent2)
        SwatchRow("field", colors.field)
        SwatchRow("onAccent", colors.onAccent)
        MonoText("fond d'écran", color = colors.muted, size = LauncherStyle.textSmall)
        if (wallpaper == null) {
            MonoText("indisponible", size = LauncherStyle.textSmall)
        } else {
            SwatchRow("primaire", wallpaper.primary)
            SwatchRow("secondaire", wallpaper.secondary)
            SwatchRow("tertiaire", wallpaper.tertiary)
            MonoText("hints ${wallpaper.hints}", size = LauncherStyle.textSmall, maxLines = 1)
        }
        MonoText("changements $changes", color = colors.muted, size = LauncherStyle.textSmall, maxLines = 1)
    }
}

/** « ■ accent   #FFB4C5E8 » ; « — » si la couleur n'existe pas. */
@Composable
private fun SwatchRow(name: String, color: Color?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val shape = RoundedCornerShape(2.dp)
        Box(
            Modifier
                .size(10.dp)
                .background(color ?: Color.Transparent, shape)
                .border(LauncherStyle.lineWidth, LauncherTheme.colors.border, shape),
        )
        MonoText(
            "${name.padEnd(10)} ${color?.let { hex(it) } ?: "—"}",
            size = LauncherStyle.textSmall,
            maxLines = 1,
        )
    }
}

private fun hex(color: Color): String = "#%08X".format(color.toArgb())
