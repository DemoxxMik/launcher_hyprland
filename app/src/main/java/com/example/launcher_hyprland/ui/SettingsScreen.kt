package com.example.launcher_hyprland.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.launcher_hyprland.data.Contrast
import com.example.launcher_hyprland.data.IconMode
import com.example.launcher_hyprland.data.LauncherSettings
import com.example.launcher_hyprland.ui.widgets.MonoText
import com.example.launcher_hyprland.ui.widgets.TileLabel
import kotlin.math.roundToInt

/**
 * Écran « réglages », en plein écran (à la place de la disposition), dans le style des cases.
 * Chaque choix s'applique tout de suite (sauvegarde + redessin) : l'écran lui-même change sous le doigt.
 * Ouvert par « réglages » dans la barre d'édition, ou par un appui long sur la barre des espaces.
 * Le flou n'y est pas : à évaluer plus tard (voir docs/8-ajustements.md, 8.6.4).
 */
@Composable
fun SettingsScreen(onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val settings = LauncherTheme.settings
    val edit = LocalSettingsEditor.current
    val colors = LauncherTheme.colors
    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(LauncherStyle.gap)
            .tileShadow(shape, LauncherTheme.shadow)
            .clip(shape)
            .background(colors.tile, shape)
            .border(LauncherStyle.borderWidth, colors.accent, shape)
            .padding(LauncherStyle.tilePadding),
    ) {
        // L'étiquette de la case se dessine en pastille d'accent : c'est la case « active »
        CompositionLocalProvider(LocalTileActive provides true) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(LauncherStyle.lineSpacing * 2),
            ) {
                TileLabel("réglages")
                SettingsContent(settings, edit)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    BarButton("ok", onClick = onClose)
                }
            }
        }
    }
}

@Composable
private fun SettingsContent(s: LauncherSettings, edit: (LauncherSettings) -> Unit) {
    SettingRow(
        "cases",
        listOf(false to "sombres", true to "claires"),
        s.lightTiles,
    ) { edit(s.copy(lightTiles = it)) }

    SettingRow(
        "coins",
        LauncherStyle.cornerChoices.map { it to "$it dp" },
        s.cornerRadius,
    ) { edit(s.copy(cornerRadius = it)) }

    SettingRow(
        "texte",
        LauncherStyle.fontScaleChoices.map { it to "${(it * 100).roundToInt()} %" },
        s.fontScale,
    ) { edit(s.copy(fontScale = it)) }

    SettingRow(
        "ombre",
        listOf(false to "sans", true to "décalée"),
        s.shadow,
    ) { edit(s.copy(shadow = it)) }

    SettingRow(
        "icônes des apps",
        listOf(IconMode.MONOCHROME to "mono", IconMode.SILHOUETTE to "silhouette", IconMode.NONE to "sans"),
        s.icons,
    ) { edit(s.copy(icons = it)) }

    SettingRow(
        "contraste",
        listOf(Contrast.SOFT to "doux", Contrast.NORMAL to "normal", Contrast.HIGH to "contrasté"),
        s.contrast,
    ) { edit(s.copy(contrast = it)) }

    SettingRow(
        "teinte des fonds",
        LauncherStyle.tintChoices.indices.map { it to it.toString() },
        s.tint,
    ) { edit(s.copy(tint = it)) }
}

/** Un réglage : son nom (atténué), puis ses choix en pastilles ; le choix courant est rempli d'accent. */
@Composable
private fun <T> SettingRow(title: String, choices: List<Pair<T, String>>, current: T, onPick: (T) -> Unit) {
    val colors = LauncherTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        MonoText(title, color = colors.muted, size = LauncherStyle.textSmall, maxLines = 1)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            for ((value, label) in choices) {
                val on = value == current
                val pill = RoundedCornerShape(percent = 50)
                Box(
                    modifier = Modifier
                        .clip(pill)
                        .background(if (on) colors.accent else colors.field, pill)
                        .clickable { onPick(value) }
                        .padding(horizontal = LauncherStyle.pillPaddingH, vertical = 6.dp),
                ) {
                    MonoText(label, color = if (on) colors.onAccent else colors.text, maxLines = 1)
                }
            }
        }
    }
}

@Preview(widthDp = 412, heightDp = 915, showBackground = true, backgroundColor = 0xFF5A6B85)
@Composable
private fun SettingsScreenPreview() {
    LauncherTheme { SettingsScreen(onClose = {}) }
}
